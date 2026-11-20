package org.example.tools;

import org.assertj.core.api.Assertions;
import org.example.loader.FinancialDataLoader;
import org.example.statements.FinancialStatements;
import org.example.statements.FinancialStatementRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EdgarDataLoaderToolTest {
    private static final String TICKER = "AAPL";
    private static final FinancialStatements TEST_FIXTURE = new FinancialStatements(
            TICKER,
            "TEST_FIXTURE",
            List.of("https://edgar.com"),
            List.of(
                    new FinancialStatements.StatementTable("Income Statement", List.of("2024", "2023"),
                            List.of(new FinancialStatements.StatementRow("Revenue", revenues()))),
                    new FinancialStatements.StatementTable("Balance Sheet", List.of("2024"),
                            List.of(new FinancialStatements.StatementRow("Total assets", Map.of("2024", "359,241,000")))),
                    new FinancialStatements.StatementTable("Cash Flow Statement", List.of("2024"),
                            List.of(new FinancialStatements.StatementRow("Free cash flow", Map.of("2024", "0"))))
            )
    );

    @Test
    void validInputArgumentsAreAccepted() {
        EdgarDataLoaderTool tool = new EdgarDataLoaderTool(financialDataLoaderMock());
        FinancialStatements financialStatements = tool.getFinancialStatements(new FinancialStatementRequest(TICKER, 2));

        Assertions.assertThat(financialStatements).isEqualTo(TEST_FIXTURE);
    }

    private FinancialDataLoader financialDataLoaderMock() {
        FinancialDataLoader dataLoader = mock(FinancialDataLoader.class);
        when(dataLoader.load(new FinancialStatementRequest(TICKER, 2))).thenReturn(TEST_FIXTURE);
        return dataLoader;
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void nullOrBlankTickerIsNotAllowed(String ticker) {
        Assertions.assertThatThrownBy(() -> {
            new EdgarDataLoaderTool(request -> {
                throw new AssertionError("Loader should not be called with invalid input.");
            })
                    .getFinancialStatements(new FinancialStatementRequest(ticker, 2));
        }).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid stock ticker: [%s]".formatted(ticker));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 5})
    void annualPeriodsInAcceptedRange(int annualPeriods) {
        Assertions.assertThatThrownBy(() -> {
                    new EdgarDataLoaderTool(request -> {
                        throw new AssertionError("Loader should not be called with invalid input.");
                    })
                            .getFinancialStatements(new FinancialStatementRequest(TICKER, annualPeriods));
                }).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid annualPeriods value: [%d]".formatted(annualPeriods));
    }

    private static Map<String, String> revenues() {
        Map<String, String> revenues = new LinkedHashMap<>();
        revenues.put("2024", "466,823,000");
        revenues.put("2023", null);
        return revenues;
    }


}
