package org.example.crawler.dividendwatch;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DividendWatchBrowserLogin {
    private final BrowserLoginConfig config;

    public DividendWatchBrowserLogin(BrowserLoginConfig config) {
        this.config = config;
    }

    public static DividendWatchBrowserLogin fromEnvironment() {
        return new DividendWatchBrowserLogin(BrowserLoginConfig.fromEnvironment());
    }

    public BrowserLoginResult login() {
        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(config.headless());
            if (config.browserExecutablePath() != null && !config.browserExecutablePath().isBlank()) {
                launchOptions.setExecutablePath(Path.of(config.browserExecutablePath()));
            }
            try (Browser browser = playwright.chromium().launch(launchOptions)) {
                BrowserContext context = browser.newContext();
                Page page = context.newPage();
                page.setDefaultTimeout(config.timeout().toMillis());
                page.navigate(config.loginUrl());
                waitForDom(page);
                debug(page, "opened " + page.url());

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
                if (config.postLoginUrl() != null && !config.postLoginUrl().isBlank()) {
                    page.navigate(config.postLoginUrl());
                    waitForDom(page);
                }
                debug(page, "login ended at " + page.url());

                Map<String, String> cookies = toCookieMap(context.cookies());
                return new BrowserLoginResult(!cookies.isEmpty(), page.url(), cookies);
            }
        }
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

    private void waitForDom(Page page) {
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
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
            String baseUrl = env("DIVIDENDWATCH_BASE_URL", DividendWatchCrawler.DEFAULT_BASE_URL);
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
