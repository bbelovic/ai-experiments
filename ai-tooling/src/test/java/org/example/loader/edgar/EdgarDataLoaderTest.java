package org.example.loader.edgar;

import org.example.statements.FinancialStatements;
import org.example.statements.FinancialStatements.StatementRow;
import org.example.statements.FinancialStatements.StatementTable;
import org.example.statements.balancesheet.EdgarBalanceSheetService;
import org.example.statements.cashflow.EdgarCashFlowStatementService;
import org.example.statements.income.EdgarIncomeStatementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdgarDataLoaderTest {
    private static final String TICKER = "AAPL";
    private static final FinancialStatements BALANCE_SHEET = new FinancialStatements(TICKER, "EDGAR",
            List.of("https://www.sec.gov/Archives/edgar/data/320193/000032019325000079/aapl-20250927.htm"),
            List.of(new StatementTable("Balance Sheet", List.of("2025"),
                    List.of(new StatementRow("Total assets", Map.of("2025", "359,241,000"))))));
    private static final FinancialStatements INCOME_STATEMENT = new FinancialStatements(TICKER, "EDGAR",
            List.of(
                    "https://www.sec.gov/Archives/edgar/data/320193/000032019325000079/aapl-20250927.htm",
                    "https://www.sec.gov/Archives/edgar/data/320193/000032019324000123/aapl-20240928.htm"),
            List.of(new StatementTable("Income Statement", List.of("2025", "2024"),
                    List.of(new StatementRow("Revenue", Map.of("2025", "466,823,000", "2024", "391,035,000"))))));
    private static final FinancialStatements CASH_FLOW_STATEMENT = new FinancialStatements(TICKER, "EDGAR",
            List.of("https://www.sec.gov/Archives/edgar/data/320193/000032019325000079/aapl-20250927.htm"),
            List.of(new StatementTable("Cash Flow Statement", List.of("2025"),
                    List.of(new StatementRow("Free cash flow", Map.of("2025", "143,482,000"))))));

    @Mock
    private EdgarBalanceSheetService balanceSheetService;
    @Mock
    private EdgarIncomeStatementService incomeStatementService;
    @Mock
    private EdgarCashFlowStatementService cashFlowService;

    @Test
    void loaderReturnFinancialStatementData() {
        var loader = new EdgarDataLoader(TICKER, balanceSheetService, incomeStatementService, cashFlowService);
        when(balanceSheetService.annualBalanceSheetStatement(TICKER))
                .thenReturn(BALANCE_SHEET);
        when(incomeStatementService.annualIncomeStatement(TICKER))
                .thenReturn(INCOME_STATEMENT);
        when(cashFlowService.annualCashFlowStatement(TICKER))
                .thenReturn(CASH_FLOW_STATEMENT);
        var actual = loader.load();

        assertThat(actual.ticker()).isEqualTo(TICKER);
        assertThat(actual.sourceUrl()).containsAll(
                Stream.of(INCOME_STATEMENT.sourceUrl(), BALANCE_SHEET.sourceUrl(), CASH_FLOW_STATEMENT.sourceUrl())
                        .flatMap(Collection::stream).distinct().toList()
        );
        assertThat(actual.statements()).hasSize(3)
                .containsAll(Stream.of(INCOME_STATEMENT.statements(), BALANCE_SHEET.statements(), CASH_FLOW_STATEMENT.statements())
                        .flatMap(Collection::stream).toList());

    }

}
