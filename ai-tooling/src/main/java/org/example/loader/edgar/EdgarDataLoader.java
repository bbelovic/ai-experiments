package org.example.loader.edgar;

import org.example.loader.FinancialDataLoader;
import org.example.statements.FinancialStatements;
import org.example.statements.FinancialStatementRequest;
import org.example.statements.balancesheet.EdgarBalanceSheetService;
import org.example.statements.cashflow.EdgarCashFlowStatementService;
import org.example.statements.income.EdgarIncomeStatementService;

import java.util.Collection;
import java.util.stream.Stream;

public class EdgarDataLoader implements FinancialDataLoader {
    private final EdgarBalanceSheetService balanceSheetService;
    private final EdgarIncomeStatementService incomeStatementService;
    private final EdgarCashFlowStatementService cashFlowStatementService;

    public EdgarDataLoader(EdgarBalanceSheetService balanceSheetService,
                           EdgarIncomeStatementService incomeStatementService,
                           EdgarCashFlowStatementService cashFlowStatementService) {
        this.balanceSheetService = balanceSheetService;
        this.incomeStatementService = incomeStatementService;
        this.cashFlowStatementService = cashFlowStatementService;
    }


    @Override
    public FinancialStatements load(FinancialStatementRequest request) {
        FinancialStatements balanceSheetStatement = balanceSheetService.annualBalanceSheetStatement(request);
        FinancialStatements incomeStatement = incomeStatementService.annualIncomeStatement(request);
        FinancialStatements cashFlowStatement = cashFlowStatementService.annualCashFlowStatement(request);
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
