package org.example.crawler.dividendwatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.statements.FinancialStatements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public final class DividendWatchCrawlerApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(DividendWatchCrawlerApp.class);
    private DividendWatchCrawlerApp() {
    }

    static void main(String[] args) throws Exception {
        String ticker = stockTicker(args);
        DividendWatchBrowserLogin browser = DividendWatchBrowserLogin.fromSystemProperties();
        FinancialStatements statements = browser.scrapeFinancialStatements(ticker);
        String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(statements);
        String outputPath = System.getProperty("dividendwatch.statements.output");
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
    }

    private static String stockTicker(String[] args) {
        if (args.length > 0 && !args[0].isBlank()) {
            return args[0].trim();
        }
        String fromProperty = System.getProperty("dividendwatch.stock.ticker");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        throw new IllegalArgumentException(
                "Missing stock ticker. Pass it as the first argument (e.g. AAPL) or set -Ddividendwatch.stock.ticker=AAPL.");
    }
}
