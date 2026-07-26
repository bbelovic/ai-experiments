package org.example.crawler.dividendwatch;

import java.nio.file.Path;

public final class DividendWatchCrawlerApp {
    private DividendWatchCrawlerApp() {
    }

    public static void main(String[] args) throws Exception {
        DividendWatchCrawler crawler = DividendWatchCrawler.fromEnvironment();
        DividendWatchCrawler.LoginResult login = crawler.login();
        if (!login.successful()) {
            throw new IllegalStateException("Dividend Watch login failed. Final URL: " + login.finalUrl());
        }

        String downloadUrl = System.getenv("DIVIDENDWATCH_DOWNLOAD_URL");
        if (downloadUrl != null && !downloadUrl.isBlank()) {
            String target = env("DIVIDENDWATCH_DOWNLOAD_TARGET", "build/dividendwatch-download");
            Path downloaded = crawler.download(downloadUrl, Path.of(target));
            System.out.println("Downloaded " + downloadUrl + " to " + downloaded.toAbsolutePath());
            return;
        }

        String scrapeUrl = env("DIVIDENDWATCH_SCRAPE_URL", DividendWatchCrawler.DEFAULT_BASE_URL + "/my-stocks");
        String scrapeSelector = env("DIVIDENDWATCH_SCRAPE_SELECTOR", "body");
        System.out.println(crawler.scrapeText(scrapeUrl, scrapeSelector));
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
