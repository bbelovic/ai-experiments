package org.example.crawler.dividendwatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public final class DividendWatchCrawlerApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(DividendWatchCrawlerApp.class);
    private DividendWatchCrawlerApp() {
    }

    static void main(String[] args) throws Exception {
        DividendWatchBrowserLogin browser = DividendWatchBrowserLogin.fromEnvironment();
        String ticker = stockTicker(args);
        if (ticker != null) {
            DividendWatchFinancialStatements statements = browser.scrapeFinancialStatements(ticker);
            String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(statements);
            String outputPath = System.getenv("DIVIDENDWATCH_STATEMENTS_OUTPUT");
            if (outputPath != null && !outputPath.isBlank()) {
                Path target = Path.of(outputPath);
                Path parent = target.toAbsolutePath().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(target, json);
                LOGGER.info("Wrote financial statements for [{}] to [{}]", ticker.toUpperCase(), target.toAbsolutePath());
            } else {
                LOGGER.info(json);
            }
            return;
        }

        String downloadUrl = System.getenv("DIVIDENDWATCH_DOWNLOAD_URL");
        if (downloadUrl != null && !downloadUrl.isBlank()) {
            String target = env("DIVIDENDWATCH_DOWNLOAD_TARGET", "build/dividendwatch-download");
            Path downloaded = browser.download(downloadUrl, Path.of(target));
            LOGGER.info("Downloaded [{}] to [{}]", downloadUrl, downloaded.toAbsolutePath());
            return;
        }

        String scrapeUrl = env("DIVIDENDWATCH_SCRAPE_URL", DividendWatchBrowserLogin.DEFAULT_BASE_URL + "/tracker");
        String scrapeSelector = env("DIVIDENDWATCH_SCRAPE_SELECTOR", "body");
        LOGGER.info(browser.scrapeText(scrapeUrl, scrapeSelector));
    }

    private static String stockTicker(String[] args) {
        if (args.length > 0 && !args[0].isBlank()) {
            return args[0].trim();
        }
        String fromEnvironment = System.getenv("DIVIDENDWATCH_STOCK_TICKER");
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return fromEnvironment.trim();
        }
        return null;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
