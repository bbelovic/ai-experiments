package org.example.edgar.main;

import org.example.statements.EdgarStatement;
import org.example.statements.balancesheet.EdgarBalanceSheetService;
import org.example.statements.income.EdgarIncomeStatement;
import org.example.statements.income.EdgarIncomeStatementService;

public class Main {
    static void main(String[] args) throws Exception {
        String ticker = args.length > 0 ? args[0].trim() : env("EDGAR_STOCK_TICKER", "AAPL");
        String userAgent = env("EDGAR_USER_AGENT", "ai-experiments test@example.com");

        EdgarStatement balanceSheetStatement = new EdgarBalanceSheetService(userAgent, 4)
                .annualBalanceSheetStatement(ticker);
        EdgarIncomeStatement incomeStatement = new EdgarIncomeStatementService(userAgent, 4)
                .annualIncomeStatement(ticker);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
