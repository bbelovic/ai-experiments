package org.example.statements.income;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.statements.FinancialStatements;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.text.DecimalFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.statements.income.IncomeStatementMetricEnumType.*;

class EdgarIncomeStatementServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EdgarIncomeStatementService service = new EdgarIncomeStatementService(
            HttpClient.newHttpClient(),
            objectMapper,
            "ai-experiments test@example.com",
            4
    );

    @Test
    void extractsStatementShapedIncomeStatementFromRealAAPL10Ks() throws Exception {
        JsonNode submissions = readFixtureJson("AAPL-submissions.json");
        JsonNode companyFacts = readFixtureJson("AAPL-company-facts.json");

        FinancialStatements statements = service.extractAnnualIncomeStatement(
                "aapl",
                submissions,
                companyFacts
        );

        DecimalFormat valueFormat = EdgarIncomeStatementService.getValueFormat();

        assertThat(statements.ticker()).isEqualTo("AAPL");
        assertThat(statements.source()).isEqualTo("EDGAR");
        assertThat(statements.sourceUrl()).containsExactly(
                "https://www.sec.gov/Archives/edgar/data/320193/000032019325000079/aapl-20250927.htm",
                "https://www.sec.gov/Archives/edgar/data/320193/000032019324000123/aapl-20240928.htm",
                "https://www.sec.gov/Archives/edgar/data/320193/000032019323000106/aapl-20230930.htm",
                "https://www.sec.gov/Archives/edgar/data/320193/000032019322000108/aapl-20220924.htm");
        assertThat(statements.statements()).hasSize(1);
        assertThat(statements.statements().getFirst().name()).isEqualTo("Income Statement");
        assertThat(statements.statements().getFirst().periods()).containsExactly("2025", "2024", "2023", "2022");

        assertThat(row(statements, REVENUE).values())
                .containsEntry("2025", valueFormat.format(416161000000L))
                .containsEntry("2024", valueFormat.format(391035000000L))
                .containsEntry("2023", valueFormat.format(383285000000L))
                .containsEntry("2022", valueFormat.format(394328000000L));
        assertThat(row(statements, COST_OF_REVENUE).values())
                .containsEntry("2025", valueFormat.format(220960000000L))
                .containsEntry("2024", valueFormat.format(210352000000L))
                .containsEntry("2023", valueFormat.format(214137000000L));
        assertThat(row(statements, GROSS_PROFIT).values())
                .containsEntry("2025", valueFormat.format(195201000000L))
                .containsEntry("2024", valueFormat.format(180683000000L))
                .containsEntry("2023", valueFormat.format(169148000000L));
        assertThat(row(statements, RESEARCH_AND_DEVELOPMENT).values())
                .containsEntry("2025", valueFormat.format(34550000000L))
                .containsEntry("2024", valueFormat.format(31370000000L))
                .containsEntry("2023", valueFormat.format(29915000000L));
        assertThat(row(statements, SELLING_GENERAL_AND_ADMINISTRATIVE).values())
                .containsEntry("2025", valueFormat.format(27601000000L))
                .containsEntry("2024", valueFormat.format(26097000000L))
                .containsEntry("2023", valueFormat.format(24932000000L));
        assertThat(row(statements, OPERATING_EXPENSES).values())
                .containsEntry("2025", valueFormat.format(34550000000L + 27601000000L))
                .containsEntry("2024", valueFormat.format(31370000000L + 26097000000L))
                .containsEntry("2023", valueFormat.format(29915000000L + 24932000000L));
        assertThat(row(statements, OPERATING_INCOME).values())
                .containsEntry("2025", valueFormat.format(133050000000L))
                .containsEntry("2024", valueFormat.format(123216000000L))
                .containsEntry("2023", valueFormat.format(114301000000L));
        assertThat(row(statements, NON_OPERATING_INCOME_EXPENSE).values())
                .containsEntry("2025", valueFormat.format(-321000000L))
                .containsEntry("2024", valueFormat.format(269000000))
                .containsEntry("2023", valueFormat.format(-565000000L));
        assertThat(row(statements, NET_INCOME).values())
                .containsEntry("2025", valueFormat.format(112010000000L))
                .containsEntry("2024", valueFormat.format(93736000000L))
                .containsEntry("2023", valueFormat.format(96995000000L));
        assertThat(row(statements, DILUTED_EPS).values())
                .containsEntry("2025", valueFormat.format(7.46))
                .containsEntry("2024", valueFormat.format(6.08))
                .containsEntry("2023", valueFormat.format(6.13));
        assertThat(row(statements, DILUTED_SHARES).values())
                .containsEntry("2025", valueFormat.format(15004697000L))
                .containsEntry("2024", valueFormat.format(15408095000L))
                .containsEntry("2023", valueFormat.format(15812547000L));
    }

    private FinancialStatements.StatementRow row(FinancialStatements statements, IncomeStatementMetricEnumType metric) {
        return statements.statements().getFirst().rows().stream()
                .filter(row -> row.metric().equals(metric.getLabel()))
                .findFirst()
                .orElseThrow();
    }

    private JsonNode readFixtureJson(String name) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/fixtures/" + name)) {
            assertThat(input)
                    .as("fixture /fixtures/%s should be on the test classpath", name)
                    .isNotNull();
            return objectMapper.readTree(input);
        }
    }
}
