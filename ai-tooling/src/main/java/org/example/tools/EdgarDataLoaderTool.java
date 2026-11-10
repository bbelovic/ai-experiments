package org.example.tools;

import org.example.loader.FinancialDataLoader;
import org.example.loader.edgar.EdgarDataLoader;
import org.example.statements.FinancialStatements;
import org.example.statements.balancesheet.EdgarBalanceSheetService;
import org.example.statements.cashflow.EdgarCashFlowStatementService;
import org.example.statements.income.EdgarIncomeStatementService;

public class EdgarDataLoaderTool {

    private static final String USER_AGENT = "ai-experiments test@example.com";

    FinancialStatements getFinancialStatements(String ticker, int annualPeriods) {
        if (ticker == null || ticker.isBlank())
            throw new IllegalArgumentException("Invalid stock ticker: [%s]".formatted(ticker));
        if (annualPeriods <= 0)
            throw new IllegalArgumentException("Invalid annualPeriods value: [%d]".formatted(annualPeriods));

        EdgarBalanceSheetService balanceSheetService = new EdgarBalanceSheetService(USER_AGENT, annualPeriods);
        EdgarIncomeStatementService incomeStatementService = new EdgarIncomeStatementService(USER_AGENT, annualPeriods);
        EdgarCashFlowStatementService cashFlowStatementService = new EdgarCashFlowStatementService(USER_AGENT, annualPeriods);
        FinancialDataLoader dataLoader =  new EdgarDataLoader(ticker, balanceSheetService, incomeStatementService, cashFlowStatementService);
        return dataLoader.load();
    }
}
