package org.example.crawler.dividendwatch;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

public final class DividendWatchCrawlerApp {
    private DividendWatchCrawlerApp() {
    }

    public static void main(String[] args) throws Exception {
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
                System.out.println("Wrote financial statements for " + ticker.toUpperCase() + " to " + target.toAbsolutePath());
            } else {
                System.out.println(json);
            }
            return;
        }

        String downloadUrl = System.getenv("DIVIDENDWATCH_DOWNLOAD_URL");
        if (downloadUrl != null && !downloadUrl.isBlank()) {
            String target = env("DIVIDENDWATCH_DOWNLOAD_TARGET", "build/dividendwatch-download");
            Path downloaded = browser.download(downloadUrl, Path.of(target));
            System.out.println("Downloaded " + downloadUrl + " to " + downloaded.toAbsolutePath());
            return;
        }

        String scrapeUrl = env("DIVIDENDWATCH_SCRAPE_URL", DividendWatchBrowserLogin.DEFAULT_BASE_URL + "/tracker");
        String scrapeSelector = env("DIVIDENDWATCH_SCRAPE_SELECTOR", "body");
        System.out.println(browser.scrapeText(scrapeUrl, scrapeSelector));
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
