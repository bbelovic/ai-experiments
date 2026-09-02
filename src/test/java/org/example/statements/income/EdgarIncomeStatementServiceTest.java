package org.example.statements.income;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.text.DecimalFormat;

import static org.assertj.core.api.Assertions.assertThat;

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

        EdgarIncomeStatement statements = service.extractAnnualIncomeStatement(
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

        assertThat(row(statements, "Revenue").values())
                .containsEntry("2025", valueFormat.format(416161000000L));
        assertThat(row(statements, "Gross profit").values())
                .containsEntry("2025", valueFormat.format(195201000000L));
        assertThat(row(statements, "Operating income").values())
                .containsEntry("2025", valueFormat.format(133050000000L));
        assertThat(row(statements, "Net income").values())
                .containsEntry("2025", valueFormat.format(112010000000L));
        assertThat(row(statements, "Diluted EPS").values())
                .containsEntry("2025", valueFormat.format(7.46));
        assertThat(row(statements, "Diluted shares").values())
                .containsEntry("2025", valueFormat.format(15004697000L));
    }

    private EdgarIncomeStatement.StatementRow row(EdgarIncomeStatement statements, String metric) {
        return statements.statements().getFirst().rows().stream()
                .filter(row -> row.metric().equals(metric))
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
