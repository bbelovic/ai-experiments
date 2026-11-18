package org.example.tools;

import org.example.loader.FinancialDataLoader;
import org.example.loader.FinancialDataLoaderFactory;
import org.example.loader.edgar.EdgarDataLoader;
import org.example.statements.FinancialStatements;
import org.example.statements.balancesheet.EdgarBalanceSheetService;
import org.example.statements.cashflow.EdgarCashFlowStatementService;
import org.example.statements.income.EdgarIncomeStatementService;

import java.util.Locale;

public class EdgarDataLoaderTool {

    private static final String USER_AGENT = "ai-experiments test@example.com";

    private final FinancialDataLoaderFactory factory;

    public EdgarDataLoaderTool(FinancialDataLoaderFactory factory) {
        this.factory = factory;
    }

    FinancialStatements getFinancialStatements(String ticker, int annualPeriods) {
        if (ticker == null || ticker.isBlank())
            throw new IllegalArgumentException("Invalid stock ticker: [%s]".formatted(ticker));
        if (annualPeriods < 1 || annualPeriods > 4)
            throw new IllegalArgumentException("Invalid annualPeriods value: [%d]".formatted(annualPeriods));
//
//        EdgarBalanceSheetService balanceSheetService = new EdgarBalanceSheetService(USER_AGENT, annualPeriods);
//        EdgarIncomeStatementService incomeStatementService = new EdgarIncomeStatementService(USER_AGENT, annualPeriods);
//        EdgarCashFlowStatementService cashFlowStatementService = new EdgarCashFlowStatementService(USER_AGENT, annualPeriods);
//        FinancialDataLoader dataLoader =  new EdgarDataLoader(ticker, balanceSheetService, incomeStatementService, cashFlowStatementService);

        FinancialDataLoader dataLoader =  factory.create(ticker.trim().toUpperCase(Locale.ROOT), annualPeriods);
        return dataLoader.load();
    }
}
