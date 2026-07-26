package org.example.crawler.dividendwatch;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class DividendWatchCrawler {
    public static final String DEFAULT_BASE_URL = "https://dividend.watch";

    private final CrawlerConfig config;
    private final Map<String, String> cookies = new LinkedHashMap<>();

    public DividendWatchCrawler(CrawlerConfig config) {
        this.config = config;
    }

    public static DividendWatchCrawler fromEnvironment() {
        return new DividendWatchCrawler(CrawlerConfig.fromEnvironment());
    }

    public LoginResult login() throws IOException {
        Connection.Response loginPage = connection(config.loginUrl())
                .method(Connection.Method.GET)
                .execute();
        cookies.putAll(loginPage.cookies());

        Document loginDocument = loginPage.parse();
        Element form = findLoginForm(loginDocument)
                .orElseThrow(() -> new IllegalStateException(
                        "Could not find login form with field " + config.usernameField()));

        Map<String, String> formData = collectFormData(form);
        formData.put(config.usernameField(), config.username());
        formData.put(config.passwordField(), config.password());
        formData.putAll(config.extraLoginFields());

        String actionUrl = resolveActionUrl(config.loginUrl(), form);
        Connection.Response loginResponse = connection(actionUrl)
                .method(Connection.Method.POST)
                .data(formData)
                .followRedirects(true)
                .execute();
        cookies.putAll(loginResponse.cookies());

        boolean successful = loginResponse.statusCode() >= 200
                && loginResponse.statusCode() < 400
                && !looksLikeLoginPage(loginResponse);
        return new LoginResult(successful, loginResponse.statusCode(), loginResponse.url().toString(), Map.copyOf(cookies));
    }

    public Document getProtectedPage(String url) throws IOException {
        Connection.Response response = connection(url)
                .method(Connection.Method.GET)
                .execute();
        cookies.putAll(response.cookies());
        return response.parse();
    }

    public String scrapeText(String url, String cssSelector) throws IOException {
        return getProtectedPage(url).select(cssSelector).text();
    }

    public Path download(String url, Path target) throws IOException {
        Connection.Response response = connection(url)
                .method(Connection.Method.GET)
                .ignoreContentType(true)
                .maxBodySize(0)
                .execute();
        cookies.putAll(response.cookies());

        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(target, response.bodyAsBytes());
        return target;
    }

    public Map<String, String> cookies() {
        return Map.copyOf(cookies);
    }

    private Connection connection(String url) {
        return Jsoup.connect(url)
                .userAgent(config.userAgent())
                .timeout(Math.toIntExact(config.timeout().toMillis()))
                .cookies(cookies);
    }

    private Optional<Element> findLoginForm(Document document) {
        if (config.formSelector() != null && !config.formSelector().isBlank()) {
            return Optional.ofNullable(document.selectFirst(config.formSelector()));
        }
        return document.select("form").stream()
                .filter(form -> form.selectFirst("[name=%s]".formatted(config.usernameField())) != null)
                .filter(form -> form.selectFirst("[name=%s]".formatted(config.passwordField())) != null)
                .findFirst();
    }

    private String resolveActionUrl(String pageUrl, Element form) {
        String action = form.attr("action");
        if (action == null || action.isBlank()) {
            return pageUrl;
        }
        return URI.create(pageUrl).resolve(action).toString();
    }

    private Map<String, String> collectFormData(Element form) {
        Map<String, String> data = new LinkedHashMap<>();
        for (Element field : form.select("input[name], textarea[name], select[name]")) {
            String name = field.attr("name");
            String type = field.attr("type").toLowerCase();

            if (field.tagName().equals("select")) {
                Element selected = field.selectFirst("option[selected]");
                if (selected == null) {
                    selected = field.selectFirst("option");
                }
                data.put(name, selected == null ? "" : selected.val());
            } else if ("checkbox".equals(type) || "radio".equals(type)) {
                if (field.hasAttr("checked")) {
                    data.put(name, field.val());
                }
            } else if (!"submit".equals(type) && !"button".equals(type) && !"image".equals(type)) {
                data.put(name, field.val());
            }
        }
        return data;
    }

    private boolean looksLikeLoginPage(Connection.Response response) throws IOException {
        String finalUrl = response.url().toString();
        if (finalUrl.equals(config.loginUrl()) || finalUrl.contains("/login")) {
            Document document = response.parse();
            return findLoginForm(document).isPresent();
        }
        return false;
    }

    public record CrawlerConfig(
            String loginUrl,
            String usernameField,
            String passwordField,
            String username,
            String password,
            String formSelector,
            Map<String, String> extraLoginFields,
            String userAgent,
            Duration timeout
    ) {
        public CrawlerConfig {
            require(loginUrl, "loginUrl");
            require(usernameField, "usernameField");
            require(passwordField, "passwordField");
            require(username, "username");
            require(password, "password");
            extraLoginFields = extraLoginFields == null ? Map.of() : Map.copyOf(extraLoginFields);
            userAgent = userAgent == null || userAgent.isBlank()
                    ? "Mozilla/5.0 (compatible; DividendWatchCrawler/1.0)"
                    : userAgent;
            timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        }

        public static CrawlerConfig fromEnvironment() {
            String baseUrl = env("DIVIDENDWATCH_BASE_URL", DEFAULT_BASE_URL);
            return new CrawlerConfig(
                    env("DIVIDENDWATCH_LOGIN_URL", baseUrl + "/login"),
                    env("DIVIDENDWATCH_USERNAME_FIELD", "email"),
                    env("DIVIDENDWATCH_PASSWORD_FIELD", "password"),
                    requiredEnv("DIVIDENDWATCH_USERNAME"),
                    requiredEnv("DIVIDENDWATCH_PASSWORD"),
                    System.getenv("DIVIDENDWATCH_LOGIN_FORM_SELECTOR"),
                    Map.of(),
                    env("DIVIDENDWATCH_USER_AGENT", "Mozilla/5.0 (compatible; DividendWatchCrawler/1.0)"),
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

    public record LoginResult(
            boolean successful,
            int statusCode,
            String finalUrl,
            Map<String, String> cookies
    ) {
    }
}
