package org.example.loader.dividendwatch;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfSystemProperty(named = "dividendwatch.browser.executable.path", matches = ".+")
class DividendWatchBrowserLoginTest {
    private static final String PASSWORD = "test-$variable_>value\\with spaces";
    private final AtomicInteger protectedRequests = new AtomicInteger();
    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String action = switch (path) {
                case "/success" -> "document.querySelector('form').remove()";
                case "/partial" -> "document.querySelector('input[name=email]').remove()";
                default -> "document.querySelector('[role=alert]').textContent='Invalid credentials'";
            };
            if (path.equals("/protected")) {
                protectedRequests.incrementAndGet();
            }
            byte[] body = """
                    <!doctype html><html><body>
                    <form onsubmit="event.preventDefault(); %s">
                        <input name="email" type="text">
                        <input name="password" type="password">
                        <button type="submit">Login with e-mail</button>
                    </form>
                    <p role="alert"></p>
                    </body></html>
                    """.formatted(action).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void rejectedLoginStopsBeforeScrapingAndRedactsInputValues() {
        assertThatThrownBy(() -> loginAt("/rejected").scrapeText(url("/protected"), "body"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("login did not complete")
                .hasMessageContaining("[REDACTED]")
                .hasMessageNotContaining(PASSWORD)
                .hasMessageNotContaining("test@example.com");
        assertThat(protectedRequests.get()).isZero();
    }

    @Test
    void disappearingUsernameAloneDoesNotCountAsSuccessfulLogin() {
        assertThatThrownBy(() -> loginAt("/partial").login())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("login did not complete");
    }

    @Test
    void disappearingLoginFormAllowsLoginToComplete() {
        assertThat(loginAt("/success").login().successful()).isTrue();
    }

    private DividendWatchBrowserLogin loginAt(String path) {
        return new DividendWatchBrowserLogin(new DividendWatchBrowserLogin.BrowserLoginConfig(
                url(path), "input[name=email]", "input[name=password]", "test@example.com", PASSWORD,
                null, "button[type=submit]", null,
                System.getProperty("dividendwatch.browser.executable.path"), null,
                Duration.ZERO, false, true, Duration.ofSeconds(2)));
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }
}
