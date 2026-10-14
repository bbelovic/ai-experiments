package org.example.statements.cashflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.statements.FinancialStatements;

public final class EdgarCashFlowStatementExtractorApp {
    private EdgarCashFlowStatementExtractorApp() {
    }

    static void main(String[] args) throws Exception {
        String ticker = args.length > 0 ? args[0].trim() : property("stock.ticker", "AAPL");
        String userAgent = property("edgar.user.agent", "ai-experiments test@example.com");

        FinancialStatements statements = new EdgarCashFlowStatementService(userAgent, 4)
                .annualCashFlowStatement(ticker);

        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(statements));
    }

    private static String property(String name, String fallback) {
        String value = System.getProperty(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
