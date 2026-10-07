package org.example.statements.balancesheet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.statements.FinancialStatements;

import java.nio.file.Files;
import java.nio.file.Path;

public final class EdgarBalanceSheetExtractorApp {
    private EdgarBalanceSheetExtractorApp() {
    }

    static void main(String[] args) throws Exception {
        String ticker = args.length > 0 ? args[0].trim() : property("stock.ticker", "AAPL");
        String userAgent = property("edgar.user.agent", "ai-experiments test@example.com");

        FinancialStatements statements = new EdgarBalanceSheetService(userAgent, 4)
                .annualBalanceSheetStatement(ticker);

        var outputFilePath = env("org.example.output.filepath", "");
        if (outputFilePath.isBlank()) {
            System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(statements));
        } else {
            var out = Files.newOutputStream(Path.of(outputFilePath));
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(out, statements);
        }
    }

    private static String property(String name, String fallback) {
        String value = System.getProperty(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
