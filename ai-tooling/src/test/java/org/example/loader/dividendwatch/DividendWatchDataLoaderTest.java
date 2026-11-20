package org.example.loader.dividendwatch;

import org.example.statements.FinancialStatementRequest;
import org.example.statements.FinancialStatements;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DividendWatchDataLoaderTest {
    @Test
    void appliesEachRequestsPeriodLimitWithoutLosingZeroOrMissingValues() {
        var login = mock(DividendWatchBrowserLogin.class);
        var original = fixture();
        when(login.scrapeFinancialStatements("DEMO")).thenReturn(original);
        var loader = new DividendWatchDataLoader(login);

        var onePeriod = loader.load(new FinancialStatementRequest(" demo ", 1));
        var twoPeriods = loader.load(new FinancialStatementRequest("DEMO", 2));

        assertThat(onePeriod.statements().getFirst().periods()).containsExactly("2024");
        assertThat(onePeriod.statements().getFirst().rows().getFirst().values())
                .containsExactlyEntriesOf(Map.of("2024", "0"));
        assertThat(twoPeriods.statements().getFirst().periods()).containsExactly("2024", "2023");
        assertThat(twoPeriods.statements().getFirst().rows().getFirst().values())
                .hasSize(2).containsEntry("2024", "0").containsEntry("2023", null);
        assertThat(twoPeriods.sourceUrl()).isEqualTo(original.sourceUrl());
        assertThat(original.statements().getFirst().periods()).hasSize(3);
        assertThat(original.statements().getFirst().rows().getFirst().values()).hasSize(3);
    }

    @Test
    void preservesAllScrapedPeriodsForTheExistingCli() {
        var login = mock(DividendWatchBrowserLogin.class);
        var original = fixture();
        when(login.scrapeFinancialStatements("DEMO")).thenReturn(original);

        var actual = new DividendWatchDataLoader(login)
                .load(new FinancialStatementRequest("DEMO", Integer.MAX_VALUE));

        assertThat(actual).isEqualTo(original);
    }

    private static FinancialStatements fixture() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("2024", "0");
        values.put("2023", null);
        values.put("2022", "10");
        return new FinancialStatements("DEMO", "TEST_FIXTURE", List.of("https://example.test/financials"),
                List.of(new FinancialStatements.StatementTable("Cash Flow Statement",
                        List.of("2024", "2023", "2022"),
                        List.of(new FinancialStatements.StatementRow("Free cash flow", values)))));
    }
}
