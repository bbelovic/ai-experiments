package org.example.loader.edgar;

import org.example.loader.FinancialDataLoader;
import org.example.statements.FinancialStatements;
import org.example.statements.balancesheet.EdgarBalanceSheetService;
import org.example.statements.cashflow.EdgarCashFlowStatementService;
import org.example.statements.income.EdgarIncomeStatementService;

import java.util.Collection;
import java.util.stream.Stream;

public class EdgarDataLoader implements FinancialDataLoader {
    private final String ticker;
    private final EdgarBalanceSheetService balanceSheetService;
    private final EdgarIncomeStatementService incomeStatementService;
    private final EdgarCashFlowStatementService cashFlowStatementService;

    public EdgarDataLoader(String ticker, EdgarBalanceSheetService balanceSheetService,
                           EdgarIncomeStatementService incomeStatementService,
                           EdgarCashFlowStatementService cashFlowStatementService) {
        this.ticker = ticker;
        this.balanceSheetService = balanceSheetService;
        this.incomeStatementService = incomeStatementService;
        this.cashFlowStatementService = cashFlowStatementService;
    }

    @Override
    public FinancialStatements load() {
        FinancialStatements balanceSheetStatement = balanceSheetService.annualBalanceSheetStatement(ticker);
        FinancialStatements incomeStatement = incomeStatementService.annualIncomeStatement(ticker);
        FinancialStatements cashFlowStatement = cashFlowStatementService.annualCashFlowStatement(ticker);
        return new FinancialStatements(
                incomeStatement.ticker(),
                incomeStatement.source(),
                Stream.of(incomeStatement.sourceUrl(), balanceSheetStatement.sourceUrl(), cashFlowStatement.sourceUrl())
                        .flatMap(Collection::stream).distinct().toList(),
                Stream.of(incomeStatement.statements(), balanceSheetStatement.statements(), cashFlowStatement.statements())
                        .flatMap(Collection::stream)
                        .toList());
    }
}
