package org.example.edgar.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.statements.FinancialStatements;
import org.example.statements.balancesheet.EdgarBalanceSheetService;
import org.example.statements.income.EdgarIncomeStatementService;

import java.util.stream.Stream;

public class Main {
    static void main(String[] args) throws Exception {
        String ticker = args.length > 0 ? args[0].trim() : env("EDGAR_STOCK_TICKER", "AAPL");
        String userAgent = env("EDGAR_USER_AGENT", "ai-experiments test@example.com");

        FinancialStatements balanceSheetStatement = new EdgarBalanceSheetService(userAgent, 4)
                .annualBalanceSheetStatement(ticker);
        FinancialStatements incomeStatement = new EdgarIncomeStatementService(userAgent, 4)
                .annualIncomeStatement(ticker);

        FinancialStatements statements = new FinancialStatements(
                incomeStatement.ticker(),
                incomeStatement.source(),
                Stream.concat(incomeStatement.sourceUrl().stream(), balanceSheetStatement.sourceUrl().stream())
                        .distinct().toList(),
                Stream.concat(incomeStatement.statements().stream(), balanceSheetStatement.statements().stream())
                        .toList()
        );
        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(statements));
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
