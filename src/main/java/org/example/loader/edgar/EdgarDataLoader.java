package org.example.loader.edgar;

import org.example.loader.FinancialDataLoader;
import org.example.statements.FinancialStatements;
import org.example.statements.balancesheet.EdgarBalanceSheetService;
import org.example.statements.income.EdgarIncomeStatementService;

import java.util.stream.Stream;

public class EdgarDataLoader implements FinancialDataLoader {
    private final String ticker;
    private final EdgarBalanceSheetService balanceSheetService;
    private final EdgarIncomeStatementService incomeStatementService;

    public EdgarDataLoader(String ticker, EdgarBalanceSheetService balanceSheetService,
                           EdgarIncomeStatementService incomeStatementService) {
        this.ticker = ticker;
        this.balanceSheetService = balanceSheetService;
        this.incomeStatementService = incomeStatementService;
    }

    @Override
    public FinancialStatements load() {
        FinancialStatements balanceSheetStatement = balanceSheetService.annualBalanceSheetStatement(ticker);
        FinancialStatements incomeStatement = incomeStatementService.annualIncomeStatement(ticker);
        return new FinancialStatements(
                incomeStatement.ticker(),
                incomeStatement.source(),
                Stream.concat(incomeStatement.sourceUrl().stream(), balanceSheetStatement.sourceUrl().stream())
                        .distinct().toList(),
                Stream.concat(incomeStatement.statements().stream(), balanceSheetStatement.statements().stream())
                        .toList());
    }
}
