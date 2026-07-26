package org.example.crawler.dividendwatch;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DividendWatchCrawlerTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void logsInWithHiddenTokenAndUsesSessionForScrapeAndDownload() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/login", this::handleLogin);
        server.createContext("/my-stocks", this::handleMyStocks);
        server.createContext("/export.csv", this::handleExport);
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        DividendWatchCrawler crawler = new DividendWatchCrawler(new DividendWatchCrawler.CrawlerConfig(
                baseUrl + "/login",
                "email",
                "password",
                "investor@example.com",
                "secret",
                null,
                Map.of(),
                "JUnit",
                Duration.ofSeconds(5)
        ));

        DividendWatchCrawler.LoginResult login = crawler.login();
        String holdings = crawler.scrapeText(baseUrl + "/my-stocks", "#holdings");
        Path download = crawler.download(baseUrl + "/export.csv", Path.of("target/dividendwatch-test/export.csv"));

        assertThat(login.successful()).isTrue();
        assertThat(login.cookies()).containsEntry("dw_session", "authenticated");
        assertThat(holdings).isEqualTo("AAPL 12 shares KO 8 shares");
        assertThat(download).hasContent("symbol,shares\nAAPL,12\nKO,8\n");
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().add("Set-Cookie", "csrf_seed=abc; Path=/");
            send(exchange, 200, """
                    <html><body>
                    <form method="post" action="/login">
                        <input type="hidden" name="_token" value="abc">
                        <input name="email">
                        <input type="password" name="password">
                    </form>
                    </body></html>
                    """);
            return;
        }

        Map<String, String> form = parseForm(exchange);
        if ("abc".equals(form.get("_token"))
                && "investor@example.com".equals(form.get("email"))
                && "secret".equals(form.get("password"))) {
            exchange.getResponseHeaders().add("Set-Cookie", "dw_session=authenticated; Path=/");
            exchange.getResponseHeaders().add("Location", "/my-stocks");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
            return;
        }

        send(exchange, 401, "Invalid credentials");
    }

    private void handleMyStocks(HttpExchange exchange) throws IOException {
        if (!authenticated(exchange)) {
            exchange.getResponseHeaders().add("Location", "/login");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
            return;
        }

        send(exchange, 200, """
                <html><body>
                    <div id="holdings">
                        <span>AAPL</span> <span>12 shares</span>
                        <span>KO</span> <span>8 shares</span>
                    </div>
                </body></html>
                """);
    }

    private void handleExport(HttpExchange exchange) throws IOException {
        if (!authenticated(exchange)) {
            exchange.sendResponseHeaders(403, -1);
            exchange.close();
            return;
        }

        exchange.getResponseHeaders().add("Content-Type", "text/csv");
        send(exchange, 200, "symbol,shares\nAAPL,12\nKO,8\n");
    }

    private boolean authenticated(HttpExchange exchange) {
        return exchange.getRequestHeaders()
                .getOrDefault("Cookie", java.util.List.of())
                .stream()
                .anyMatch(cookie -> cookie.contains("dw_session=authenticated"));
    }

    private Map<String, String> parseForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (body.isBlank()) {
            return Map.of();
        }
        return Arrays.stream(body.split("&"))
                .map(parameter -> parameter.split("=", 2))
                .collect(Collectors.toMap(
                        parameter -> decode(parameter[0]),
                        parameter -> parameter.length == 2 ? decode(parameter[1]) : ""
                ));
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
