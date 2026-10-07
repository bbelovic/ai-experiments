package org.example.statements.income;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.statements.FinancialStatements;

public final class EdgarIncomeStatementExtractorApp {
    private EdgarIncomeStatementExtractorApp() {
    }

    static void main(String[] args) throws Exception {
        String ticker = args.length > 0 ? args[0].trim() : property("stock.ticker", "AAPL");
        String userAgent = property("edgar.user.agent", "ai-experiments test@example.com");

        FinancialStatements statements = new EdgarIncomeStatementService(userAgent, 4)
                .annualIncomeStatement(ticker);

        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(statements));
    }

    private static String property(String name, String fallback) {
        String value = System.getProperty(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
