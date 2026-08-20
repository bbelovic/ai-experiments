package org.example.crawler.dividendwatch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DividendWatchBrowserLogin {
    public static final String DEFAULT_BASE_URL = "https://dividend.watch";

    private static final List<String> STATEMENT_NAMES = List.of(
            "Income Statement",
            "Balance Sheet",
            "Cash Flow Statement"
    );

    private final BrowserLoginConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @FunctionalInterface
    private interface PageOperation<T> {
        T apply(Page page, BrowserContext context) throws IOException;
    }

    public DividendWatchBrowserLogin(BrowserLoginConfig config) {
        this.config = config;
    }

    public static DividendWatchBrowserLogin fromEnvironment() {
        return new DividendWatchBrowserLogin(BrowserLoginConfig.fromEnvironment());
    }

    public BrowserLoginResult login() {
        return withAuthenticatedPage((page, context) -> {
            Map<String, String> cookies = toCookieMap(context.cookies());
            boolean loginFormStillVisible = hasVisible(page, config.usernameSelector())
                    && hasVisible(page, config.passwordSelector());
            return new BrowserLoginResult(!loginFormStillVisible, page.url(), cookies);
        });
    }

    public String scrapeText(String url, String cssSelector) {
        return withAuthenticatedPage((page, context) -> {
            if (!sameUrl(page.url(), url)) {
                page.navigate(url);
                waitForDom(page);
                page.waitForTimeout(config.postLoginSettle().toMillis());
            }
            page.waitForSelector(cssSelector, new Page.WaitForSelectorOptions()
                    .setTimeout(config.timeout().toMillis()));
            debug(page, "scraped " + url);
            return page.locator(cssSelector).first().textContent();
        });
    }

    public Path download(String url, Path target) throws IOException {
        return withAuthenticatedPage((page, context) -> {
            Path parent = target.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Download download = page.waitForDownload(() -> page.navigate(url));
            download.saveAs(target);
            debug(page, "downloaded " + url + " to " + target.toAbsolutePath());
            return target;
        });
    }

    public DividendWatchFinancialStatements scrapeFinancialStatements(String ticker) {
        String normalizedTicker = ticker.trim().toUpperCase();
        return withAuthenticatedPage((page, context) -> {
            openStockPage(page, normalizedTicker);
            openFundamentals(page);

            List<DividendWatchFinancialStatements.StatementTable> statements = new ArrayList<>();
            for (String statementName : STATEMENT_NAMES) {
                selectStatement(page, statementName);
                statements.add(extractVisibleStatementTable(page, statementName));
            }

            return new DividendWatchFinancialStatements(normalizedTicker, page.url(), List.copyOf(statements));
        });
    }

    private void openStockPage(Page page, String ticker) {
        waitForVisibleOrThrow(page, "#stock-search-navbar-input", "stock search field");
        Locator search = page.locator("#stock-search-navbar-input").first();
        search.fill(ticker);

        String stockPath = findStockPathFromSearchApi(page, ticker);
        if (stockPath != null) {
            page.navigate(origin(page.url()) + stockPath);
        } else if (clickSearchResult(page, ticker)) {
            // Navigation is triggered by the click.
        } else {
            search.press("Enter");
        }

        waitForDom(page);
        waitForStockPageOrThrow(page, ticker);
        page.waitForTimeout(config.postLoginSettle().toMillis());
        debug(page, "opened stock page for " + ticker + " at " + page.url());
    }

    private String findStockPathFromSearchApi(Page page, String ticker) {
        try {
            String json = page.evaluate("""
                    async ticker => {
                        const response = await fetch('/api/search?query=' + encodeURIComponent(ticker), {
                            credentials: 'include'
                        });
                        return await response.text();
                    }
                    """, ticker).toString();
            JsonNode root = objectMapper.readTree(json);
            return findStockPath(root, ticker).orElse(null);
        } catch (RuntimeException | IOException exception) {
            debug(page, "could not resolve stock path from search API: " + exception.getMessage());
            return null;
        }
    }

    private Optional<String> findStockPath(JsonNode node, String ticker) {
        String normalizedTicker = ticker.toLowerCase();
        if (node.isObject()) {
            Optional<String> directPath = stringFields(node)
                    .filter(value -> value.matches("(?i).*/symbol/" + normalizedTicker + "-[a-z0-9.]+.*"))
                    .map(value -> value.replaceFirst("(?i)^https?://[^/]+", ""))
                    .findFirst();
            if (directPath.isPresent()) {
                return directPath;
            }

            Optional<String> slug = stringFields(node)
                    .filter(value -> value.matches("(?i)" + normalizedTicker + "-[a-z0-9.]+"))
                    .findFirst();
            if (slug.isPresent() && objectMentionsTicker(node, ticker)) {
                return Optional.of("/symbol/" + slug.get().toLowerCase());
            }

            String exchange = textField(node, "exchange")
                    .or(() -> textField(node, "market"))
                    .or(() -> textField(node, "exchangeCode"))
                    .orElse("");
            if (!exchange.isBlank() && objectMentionsTicker(node, ticker)) {
                return Optional.of("/symbol/" + normalizedTicker + "-" + exchange.toLowerCase());
            }
        }

        if (node.isContainerNode()) {
            for (JsonNode child : node) {
                Optional<String> path = findStockPath(child, ticker);
                if (path.isPresent()) {
                    return path;
                }
            }
        }
        return Optional.empty();
    }

    private boolean objectMentionsTicker(JsonNode object, String ticker) {
        String normalizedTicker = ticker.toUpperCase();
        return stringFields(object)
                .anyMatch(value -> value.equalsIgnoreCase(normalizedTicker)
                        || value.toUpperCase().startsWith(normalizedTicker + "-"));
    }

    private java.util.stream.Stream<String> stringFields(JsonNode object) {
        List<String> values = new ArrayList<>();
        object.fields().forEachRemaining(entry -> {
            if (entry.getValue().isTextual()) {
                values.add(entry.getValue().asText());
            }
        });
        return values.stream();
    }

    private Optional<String> textField(JsonNode object, String fieldName) {
        JsonNode value = object.path(fieldName);
        return value.isTextual() && !value.asText().isBlank()
                ? Optional.of(value.asText())
                : Optional.empty();
    }

    private boolean clickSearchResult(Page page, String ticker) {
        page.waitForTimeout(1_500);

        Locator tickerLink = page.locator("a:has-text('" + ticker + "')").first();
        if (tickerLink.count() > 0 && tickerLink.isVisible()) {
            tickerLink.click();
            return true;
        }

        Locator result = page.locator("""
                div:has-text('%s'),
                li:has-text('%s'),
                [role='option']:has-text('%s'),
                [role='menuitem']:has-text('%s')
                """.formatted(ticker, ticker, ticker, ticker)).first();
        if (result.count() > 0 && result.isVisible()) {
            result.click();
            return true;
        }

        return false;
    }

    private String origin(String url) {
        URI uri = URI.create(url);
        return uri.getScheme() + "://" + uri.getHost();
    }

    private void openFundamentals(Page page) {
        clickFirstPresent(page, List.of(
                "a:has-text('Fundamentals')",
                "button:has-text('Fundamentals')",
                "text=Fundamentals"
        ));
        waitForDom(page);
        waitForVisibleOrThrow(page, "#financial-statement-select-toggle-button", "fundamentals statement selector");
        clickIfVisible(page, "button:has-text('Annual')");
        page.waitForTimeout(config.postLoginSettle().toMillis());
        debug(page, "opened fundamentals at " + page.url());
    }

    private void selectStatement(Page page, String statementName) {
        Locator nativeSelect = page.locator("select").filter(new Locator.FilterOptions().setHasText(statementName)).first();
        if (nativeSelect.count() > 0 && nativeSelect.isVisible()) {
            nativeSelect.selectOption(new SelectOption().setLabel(statementName));
        } else {
            clickFirstPresent(page, List.of(
                    "#financial-statement-select-toggle-button",
                    "button:has-text('Income Statement')",
                    "button:has-text('Balance Sheet')",
                    "button:has-text('Cash Flow Statement')",
                    "[role='button']:has-text('Income Statement')",
                    "[role='button']:has-text('Balance Sheet')",
                    "[role='button']:has-text('Cash Flow Statement')"
            ));
            clickFirstPresent(page, List.of(
                    "[role='option']:has-text('" + statementName + "')",
                    "[role='menuitem']:has-text('" + statementName + "')",
                    "li:has-text('" + statementName + "')",
                    "button:has-text('" + statementName + "')"
            ));
        }
        waitForDom(page);
        waitForVisibleOrThrow(page, "#financial-statement-select-toggle-button:has-text('" + statementName + "')",
                statementName + " selection");
        page.waitForTimeout(1_000);
        debug(page, "selected " + statementName);
    }

    private DividendWatchFinancialStatements.StatementTable extractVisibleStatementTable(Page page, String statementName) throws IOException {
        String json = page.locator("body").evaluate("""
                async () => {
                    const visible = element => {
                        const style = window.getComputedStyle(element);
                        const rect = element.getBoundingClientRect();
                        return style.visibility !== 'hidden'
                            && style.display !== 'none'
                            && rect.width > 0
                            && rect.height > 0;
                    };
                    const text = element => (element.innerText || element.textContent || '')
                        .replace(/\\u00a0/g, ' ')
                        .replace(/\\s+/g, ' ')
                        .trim();
                    const score = table => {
                        const rows = Array.from(table.querySelectorAll('tr'))
                            .map(row => Array.from(row.querySelectorAll('th,td')).map(text).filter(Boolean));
                        const all = rows.flat().join(' ');
                        const metric = /Metric/.test(all) ? 100 : 0;
                        const years = (all.match(/\\b20\\d{2}\\b/g) || []).length;
                        return metric + years + rows.length;
                    };
                    const extractFromTable = table => Array.from(table.querySelectorAll('tr'))
                        .map(row => Array.from(row.querySelectorAll('th,td')).map(text).filter(Boolean))
                        .filter(row => row.length > 1);
                    const tables = Array.from(document.querySelectorAll('table')).filter(visible);
                    if (tables.length > 0) {
                        const best = tables.sort((a, b) => score(b) - score(a))[0];
                        const scroller = (() => {
                            let current = best.parentElement;
                            while (current && current !== document.body) {
                                if (current.scrollWidth > current.clientWidth + 8) {
                                    return current;
                                }
                                current = current.parentElement;
                            }
                            return best.scrollWidth > best.clientWidth + 8 ? best : null;
                        })();
                        if (!scroller) {
                            return JSON.stringify(extractFromTable(best));
                        }

                        const wait = ms => new Promise(resolve => setTimeout(resolve, ms));
                        const mergedPeriods = [];
                        const rowsByMetric = new Map();
                        const originalScrollLeft = scroller.scrollLeft;
                        const step = Math.max(120, Math.floor(scroller.clientWidth * 0.75));
                        const positions = [];
                        for (let position = 0; position <= scroller.scrollWidth - scroller.clientWidth; position += step) {
                            positions.push(position);
                        }
                        positions.push(scroller.scrollWidth);

                        for (const position of positions) {
                            scroller.scrollLeft = position;
                            await wait(250);
                            const slice = extractFromTable(best);
                            if (slice.length === 0) {
                                continue;
                            }
                            const periods = slice[0]
                                .filter(cell => /\\b(19|20)\\d{2}\\b/.test(cell))
                                .map(cell => cell.match(/\\b(19|20)\\d{2}\\b/)[0]);
                            if (periods.length === 0) {
                                continue;
                            }
                            for (const period of periods) {
                                if (!mergedPeriods.includes(period)) {
                                    mergedPeriods.push(period);
                                }
                            }
                            for (const row of slice.slice(1)) {
                                if (row.length < 2 || !row[0]) {
                                    continue;
                                }
                                const metric = row[0];
                                const values = rowsByMetric.get(metric) || {};
                                for (let i = 0; i < periods.length; i++) {
                                    const value = row[i + 1] || '';
                                    if (value && !values[periods[i]]) {
                                        values[periods[i]] = value;
                                    }
                                }
                                rowsByMetric.set(metric, values);
                            }
                        }
                        scroller.scrollLeft = originalScrollLeft;

                        if (mergedPeriods.length === 0 || rowsByMetric.size === 0) {
                            return JSON.stringify(extractFromTable(best));
                        }
                        return JSON.stringify([
                            ['Metric', ...mergedPeriods],
                            ...Array.from(rowsByMetric.entries()).map(([metric, values]) => [
                                metric,
                                ...mergedPeriods.map(period => values[period] || '')
                            ])
                        ]);
                    }

                    const roleRows = Array.from(document.querySelectorAll('[role="row"]')).filter(visible);
                    const rows = roleRows
                        .map(row => Array.from(row.querySelectorAll('[role="cell"],[role="columnheader"],[role="rowheader"],div,span'))
                            .filter(visible)
                            .map(text)
                            .filter(Boolean)
                            .filter((value, index, values) => values.indexOf(value) === index))
                        .filter(row => row.length > 1);
                    return JSON.stringify(rows);
                }
                """).toString();

        List<List<String>> rows = objectMapper.readValue(json, new TypeReference<>() {
        });
        if (rows.isEmpty()) {
            throw new IllegalStateException("Could not extract visible table rows for " + statementName);
        }

        List<String> periods = periodsFrom(rows.getFirst());
        List<DividendWatchFinancialStatements.StatementRow> statementRows = new ArrayList<>();
        for (List<String> row : rows.subList(1, rows.size())) {
            if (row.isEmpty() || row.getFirst().isBlank()) {
                continue;
            }
            Map<String, String> values = new LinkedHashMap<>();
            for (int i = 0; i < periods.size(); i++) {
                int cellIndex = i + 1;
                values.put(periods.get(i), cellIndex < row.size() ? row.get(cellIndex) : "");
            }
            statementRows.add(new DividendWatchFinancialStatements.StatementRow(
                    row.getFirst(),
                    Collections.unmodifiableMap(new LinkedHashMap<>(values))
            ));
        }

        return new DividendWatchFinancialStatements.StatementTable(statementName, periods, List.copyOf(statementRows));
    }

    private List<String> periodsFrom(List<String> header) {
        List<String> periods = new ArrayList<>();
        for (String cell : header) {
            if (cell.matches("\\b20\\d{2}\\b") || cell.matches("\\b19\\d{2}\\b")) {
                periods.add(cell);
            }
        }
        if (!periods.isEmpty()) {
            return List.copyOf(periods);
        }
        return header.size() <= 1 ? List.of() : List.copyOf(header.subList(1, header.size()));
    }

    private <T> T withAuthenticatedPage(PageOperation<T> operation) {
        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions launchOptions = launchOptions();
            try (Browser browser = playwright.chromium().launch(launchOptions)) {
                BrowserContext context = browser.newContext();
                Page page = context.newPage();
                page.setDefaultTimeout(config.timeout().toMillis());
                attachDiagnostics(page);
                authenticate(page);
                return operation.apply(page, context);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Dividend Watch browser operation failed", exception);
        }
    }

    private BrowserType.LaunchOptions launchOptions() {
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(config.headless());
        if (config.browserExecutablePath() != null && !config.browserExecutablePath().isBlank()) {
            launchOptions.setExecutablePath(Path.of(config.browserExecutablePath()));
        }
        return launchOptions;
    }

    private void authenticate(Page page) {
        page.navigate(config.loginUrl());
        waitForDom(page);
        debug(page, "opened " + page.url());
        clickIfPresent(page, config.cookieAcceptSelector());

        if (!waitForVisible(page, config.usernameSelector(), 5_000)) {
            clickIfPresent(page, config.signInSelector());
        }
        if (!waitForVisible(page, config.usernameSelector(), 2_000)) {
            clickFirstPresent(page, List.of(
                    "a:has-text('Sign in')",
                    "button:has-text('Sign in')",
                    "a:has-text('Log in')",
                    "button:has-text('Log in')",
                    "text=Sign in",
                    "text=Log in"
            ));
        }
        waitForVisibleOrThrow(page, config.usernameSelector(), "username field");
        debug(page, "found username field at " + page.url());
        fill(page, config.usernameSelector(), config.username());
        debug(page, "filled username");
        waitForVisibleOrThrow(page, config.passwordSelector(), "password field");
        fill(page, config.passwordSelector(), config.password());
        debug(page, "filled password");

        if (config.submitSelector() == null || config.submitSelector().isBlank()) {
            page.locator(config.passwordSelector()).press("Enter");
        } else {
            clickFirstPresent(page, List.of(
                    config.submitSelector(),
                    "button:has-text('Sign in')",
                    "button:has-text('Log in')",
                    "input[type='submit']"
            ));
        }
        debug(page, "submitted login");

        waitForDom(page);
        waitForLoginToFinish(page);
        if (config.postLoginUrl() != null && !config.postLoginUrl().isBlank()) {
            page.navigate(config.postLoginUrl());
            waitForDom(page);
        }
        page.waitForTimeout(config.postLoginSettle().toMillis());
        debug(page, "login ended at " + page.url());
        debug(page, "visible controls after login:\n" + visibleControls(page));
    }

    private void attachDiagnostics(Page page) {
        if (!config.debug()) {
            return;
        }
        page.onConsoleMessage(message -> {
            if ("error".equals(message.type()) || "warning".equals(message.type())) {
                System.err.println("[dividendwatch][console][" + message.type() + "] " + message.text());
            }
        });
        page.onResponse(response -> {
            String url = response.url();
            if (url.contains("dividend.watch")
                    || url.contains("/api/")
                    || url.contains("supabase")
                    || url.contains("firebase")
                    || url.contains("auth")) {
                System.err.println("[dividendwatch][response] " + response.status() + " " + url);
            }
        });
        page.onRequestFailed(request -> System.err.println(
                "[dividendwatch][request-failed] " + request.method() + " " + request.url() + " " + request.failure()));
    }

    private void clickIfPresent(Page page, String selector) {
        if (selector == null || selector.isBlank()) {
            return;
        }
        Locator locator = page.locator(selector).first();
        if (locator.count() > 0) {
            locator.click();
            waitForDom(page);
            debug(page, "clicked " + selector);
        }
    }

    private void clickIfVisible(Page page, String selector) {
        if (selector == null || selector.isBlank()) {
            return;
        }
        Locator locator = page.locator(selector).first();
        if (locator.count() > 0 && locator.isVisible()) {
            locator.click();
            waitForDom(page);
            debug(page, "clicked " + selector);
        }
    }

    private void clickFirstPresent(Page page, List<String> selectors) {
        for (String selector : selectors) {
            if (selector == null || selector.isBlank()) {
                continue;
            }
            Locator locator = page.locator(selector).first();
            if (locator.count() > 0 && locator.isVisible()) {
                locator.click();
                waitForDom(page);
                debug(page, "clicked " + selector);
                return;
            }
        }
        throw new IllegalStateException("""
                Could not find a visible login submit/sign-in control.
                Current URL: %s
                Tried selectors: %s
                Visible controls:
                %s
                """.formatted(page.url(), selectors, visibleControls(page)));
    }

    private void fill(Page page, String selector, String value) {
        page.locator(selector).first().fill(value);
    }

    private boolean hasVisible(Page page, String selector) {
        return page.locator(selector).first().count() > 0 && page.locator(selector).first().isVisible();
    }

    private boolean waitForVisible(Page page, String selector, double timeoutMillis) {
        try {
            page.waitForSelector(selector, new Page.WaitForSelectorOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(timeoutMillis));
            return true;
        } catch (PlaywrightException exception) {
            return false;
        }
    }

    private void waitForVisibleOrThrow(Page page, String selector, String description) {
        if (!waitForVisible(page, selector, config.timeout().toMillis())) {
            throw new IllegalStateException("""
                    Could not find visible %s.
                    Current URL: %s
                    Selector: %s
                    Visible controls:
                    %s
                    """.formatted(description, page.url(), selector, visibleControls(page)));
        }
    }

    private void waitForTextOrThrow(Page page, String text, String description) {
        if (!waitForVisible(page, "text=" + text, config.timeout().toMillis())) {
            throw new IllegalStateException("""
                    Could not find %s.
                    Current URL: %s
                    Expected text: %s
                    Visible controls:
                    %s
                    """.formatted(description, page.url(), text, visibleControls(page)));
        }
    }

    private void waitForStockPageOrThrow(Page page, String ticker) {
        try {
            page.waitForFunction("""
                    ticker => {
                        const normalizedTicker = String(ticker).toUpperCase();
                        const url = window.location.href.toLowerCase();
                        const body = (document.body.innerText || '').toUpperCase();
                        return url.includes('/symbol/')
                            && (url.includes('/' + normalizedTicker.toLowerCase() + '-')
                                || body.includes(normalizedTicker));
                    }
                    """, ticker, new Page.WaitForFunctionOptions().setTimeout(config.timeout().toMillis()));
        } catch (PlaywrightException exception) {
            throw new IllegalStateException("""
                    Could not confirm stock page for %s.
                    Current URL: %s
                    Visible controls:
                    %s
                    """.formatted(ticker, page.url(), visibleControls(page)), exception);
        }
    }

    private void waitForLoginToFinish(Page page) {
        try {
            page.waitForFunction("""
                    ([usernameSelector, passwordSelector]) => {
                        const visible = selector => {
                            const element = document.querySelector(selector);
                            if (!element) {
                                return false;
                            }
                            const style = window.getComputedStyle(element);
                            const rect = element.getBoundingClientRect();
                            return style.visibility !== 'hidden'
                                && style.display !== 'none'
                                && rect.width > 0
                                && rect.height > 0;
                        };
                        return !visible(usernameSelector) || !visible(passwordSelector);
                    }
                    """, List.of(config.usernameSelector(), config.passwordSelector()),
                    new Page.WaitForFunctionOptions().setTimeout(config.timeout().toMillis()));
        } catch (PlaywrightException exception) {
            debug(page, "login form still visible after submit");
        }
    }

    private void waitForDom(Page page) {
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    private boolean sameUrl(String currentUrl, String requestedUrl) {
        String current = stripTrailingSlash(currentUrl);
        String requested = stripTrailingSlash(requestedUrl);
        return current.equals(requested);
    }

    private String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String visibleControls(Page page) {
        return page.locator("a, button, input, textarea").evaluateAll("""
                elements => elements
                    .filter(element => {
                        const style = window.getComputedStyle(element);
                        const rect = element.getBoundingClientRect();
                        return style.visibility !== 'hidden'
                            && style.display !== 'none'
                            && rect.width > 0
                            && rect.height > 0;
                    })
                    .slice(0, 40)
                    .map(element => {
                        const tag = element.tagName.toLowerCase();
                        const type = element.getAttribute('type');
                        const name = element.getAttribute('name');
                        const id = element.getAttribute('id');
                        const placeholder = element.getAttribute('placeholder');
                        const aria = element.getAttribute('aria-label');
                        const text = element.innerText || element.value || '';
                        return [
                            tag,
                            type ? `type=${type}` : '',
                            name ? `name=${name}` : '',
                            id ? `id=${id}` : '',
                            placeholder ? `placeholder=${placeholder}` : '',
                            aria ? `aria-label=${aria}` : '',
                            text.trim() ? `text=${text.trim().slice(0, 80)}` : ''
                        ].filter(Boolean).join(' ');
                    })
                    .join('\\n')
                """).toString();
    }

    private void debug(Page page, String message) {
        if (!config.debug()) {
            return;
        }
        System.err.println("[dividendwatch] " + message);
        try {
            Path directory = Path.of("target/dividendwatch-debug");
            Files.createDirectories(directory);
            Files.writeString(directory.resolve("last-page.html"), page.content());
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(directory.resolve("last-page.png"))
                    .setFullPage(true));
        } catch (RuntimeException | java.io.IOException exception) {
            System.err.println("[dividendwatch] could not write debug artifacts: " + exception.getMessage());
        }
    }

    private Map<String, String> toCookieMap(List<Cookie> cookies) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Cookie cookie : cookies) {
            result.put(cookie.name, cookie.value);
        }
        return result;
    }

    public record BrowserLoginConfig(
            String loginUrl,
            String usernameSelector,
            String passwordSelector,
            String username,
            String password,
            String signInSelector,
            String submitSelector,
            String postLoginUrl,
            String browserExecutablePath,
            String cookieAcceptSelector,
            Duration postLoginSettle,
            boolean debug,
            boolean headless,
            Duration timeout
    ) {
        public BrowserLoginConfig {
            require(loginUrl, "loginUrl");
            require(usernameSelector, "usernameSelector");
            require(passwordSelector, "passwordSelector");
            require(username, "username");
            require(password, "password");
            timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        }

        public static BrowserLoginConfig fromEnvironment() {
            String baseUrl = env("DIVIDENDWATCH_BASE_URL", DEFAULT_BASE_URL);
            return new BrowserLoginConfig(
                    env("DIVIDENDWATCH_BROWSER_LOGIN_URL", env("DIVIDENDWATCH_LOGIN_URL", baseUrl + "/my-stocks")),
                    env("DIVIDENDWATCH_USERNAME_SELECTOR", "input[name='email'], input[type='email']"),
                    env("DIVIDENDWATCH_PASSWORD_SELECTOR", "input[name='password'], input[type='password']"),
                    requiredEnv("DIVIDENDWATCH_USERNAME"),
                    requiredEnv("DIVIDENDWATCH_PASSWORD"),
                    env("DIVIDENDWATCH_SIGN_IN_SELECTOR", "text=Sign in"),
                    env("DIVIDENDWATCH_SUBMIT_SELECTOR", "button[type='submit'], input[type='submit']"),
                    System.getenv("DIVIDENDWATCH_POST_LOGIN_URL"),
                    System.getenv("DIVIDENDWATCH_BROWSER_EXECUTABLE_PATH"),
                    env("DIVIDENDWATCH_COOKIE_ACCEPT_SELECTOR", "button:has-text('Accept cookies')"),
                    Duration.ofSeconds(Long.parseLong(env("DIVIDENDWATCH_POST_LOGIN_SETTLE_SECONDS", "5"))),
                    Boolean.parseBoolean(env("DIVIDENDWATCH_DEBUG", "false")),
                    Boolean.parseBoolean(env("DIVIDENDWATCH_HEADLESS", "true")),
                    Duration.ofSeconds(Long.parseLong(env("DIVIDENDWATCH_TIMEOUT_SECONDS", "30")))
            );
        }

        private static void require(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " is required");
            }
        }

        private static String requiredEnv(String name) {
            String value = System.getenv(name);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Missing required environment variable " + name);
            }
            return value;
        }

        private static String env(String name, String fallback) {
            String value = System.getenv(name);
            return value == null || value.isBlank() ? fallback : value;
        }
    }

    public record BrowserLoginResult(
            boolean successful,
            String finalUrl,
            Map<String, String> cookies
    ) {
    }
}
