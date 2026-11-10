package org.example.statements.cashflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.statements.FinancialStatements;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.example.statements.cashflow.CashFlowStatementMetricEnumType.*;

class EdgarCashFlowStatementServiceTest {
    private static final String ACCESSION_2025 = "0000320193-25-000079";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EdgarCashFlowStatementService service = new EdgarCashFlowStatementService(
            HttpClient.newHttpClient(), objectMapper, "ai-experiments test@example.com", 4);

    @Test
    void extractsAnnualCashFlowFromRealAAPL10Ks() throws Exception {
        FinancialStatements statements = extract(readFixtureJson("AAPL-company-facts-pretty.json"));
        DecimalFormat valueFormat = EdgarCashFlowStatementService.getValueFormat();

        assertThat(statements.ticker()).isEqualTo("AAPL");
        assertThat(statements.source()).isEqualTo("EDGAR");
        assertThat(statements.sourceUrl()).containsExactly(
                "https://www.sec.gov/Archives/edgar/data/320193/000032019325000079/aapl-20250927.htm",
                "https://www.sec.gov/Archives/edgar/data/320193/000032019324000123/aapl-20240928.htm",
                "https://www.sec.gov/Archives/edgar/data/320193/000032019323000106/aapl-20230930.htm",
                "https://www.sec.gov/Archives/edgar/data/320193/000032019322000108/aapl-20220924.htm");
        assertThat(statements.statements()).hasSize(1);
        assertThat(statements.statements().getFirst().name()).isEqualTo("Cash Flow Statement");
        assertThat(statements.statements().getFirst().periods()).containsExactly("2025", "2024", "2023", "2022");
        assertThat(statements.statements().getFirst().rows())
                .extracting(FinancialStatements.StatementRow::metric)
                .containsExactlyElementsOf(Arrays.stream(CashFlowStatementMetricEnumType.values())
                        .map(CashFlowStatementMetricEnumType::getLabel).toList());

        assertThat(row(statements, NET_INCOME).values())
                .containsEntry("2025", valueFormat.format(112010000000L))
                .containsEntry("2024", valueFormat.format(93736000000L))
                .containsEntry("2023", valueFormat.format(96995000000L))
                .containsEntry("2022", valueFormat.format(99803000000L));
        assertThat(row(statements, DEPRECIATION_DEPLETION_AND_AMORTIZATION).values())
                .containsEntry("2025", valueFormat.format(11698000000L))
                .containsEntry("2024", valueFormat.format(11445000000L))
                .containsEntry("2023", valueFormat.format(11519000000L))
                .containsEntry("2022", valueFormat.format(11104000000L));
        assertThat(row(statements, SHARE_BASED_COMPENSATION).values())
                .containsEntry("2025", valueFormat.format(12863000000L))
                .containsEntry("2024", valueFormat.format(11688000000L))
                .containsEntry("2023", valueFormat.format(10833000000L))
                .containsEntry("2022", valueFormat.format(9038000000L));
        assertThat(row(statements, DEFERRED_INCOME_TAX).values())
                .containsEntry("2025", null)
                .containsEntry("2024", null)
                .containsEntry("2023", null)
                .containsEntry("2022", valueFormat.format(895000000L));
        assertThat(row(statements, OPERATING_CASH_FLOW).values())
                .containsEntry("2025", valueFormat.format(111482000000L))
                .containsEntry("2024", valueFormat.format(118254000000L))
                .containsEntry("2023", valueFormat.format(110543000000L))
                .containsEntry("2022", valueFormat.format(122151000000L));
        assertThat(row(statements, CAPITAL_EXPENDITURES).values())
                .containsEntry("2025", valueFormat.format(12715000000L))
                .containsEntry("2024", valueFormat.format(9447000000L))
                .containsEntry("2023", valueFormat.format(10959000000L))
                .containsEntry("2022", valueFormat.format(10708000000L));
        assertThat(row(statements, INVESTING_CASH_FLOW).values())
                .containsEntry("2025", valueFormat.format(15195000000L))
                .containsEntry("2024", valueFormat.format(2935000000L))
                .containsEntry("2023", valueFormat.format(3705000000L))
                .containsEntry("2022", valueFormat.format(-22354000000L));
        assertThat(row(statements, DIVIDENDS_PAID).values())
                .containsEntry("2025", valueFormat.format(15421000000L))
                .containsEntry("2024", valueFormat.format(15234000000L))
                .containsEntry("2023", valueFormat.format(15025000000L))
                .containsEntry("2022", valueFormat.format(14841000000L));
        assertThat(row(statements, SHARE_REPURCHASES).values())
                .containsEntry("2025", valueFormat.format(90711000000L))
                .containsEntry("2024", valueFormat.format(94949000000L))
                .containsEntry("2023", valueFormat.format(77550000000L))
                .containsEntry("2022", valueFormat.format(89402000000L));
        assertThat(row(statements, LONG_TERM_DEBT_PROCEEDS).values())
                .containsEntry("2025", valueFormat.format(4481000000L))
                .containsEntry("2024", valueFormat.format(0L))
                .containsEntry("2023", valueFormat.format(5228000000L))
                .containsEntry("2022", valueFormat.format(5465000000L));
        assertThat(row(statements, LONG_TERM_DEBT_REPAYMENTS).values())
                .containsEntry("2025", valueFormat.format(10932000000L))
                .containsEntry("2024", valueFormat.format(9958000000L))
                .containsEntry("2023", valueFormat.format(11151000000L))
                .containsEntry("2022", valueFormat.format(9543000000L));
        assertThat(row(statements, FINANCING_CASH_FLOW).values())
                .containsEntry("2025", valueFormat.format(-120686000000L))
                .containsEntry("2024", valueFormat.format(-121983000000L))
                .containsEntry("2023", valueFormat.format(-108488000000L))
                .containsEntry("2022", valueFormat.format(-110749000000L));
        assertThat(row(statements, NET_CHANGE_IN_CASH_INCLUDING_RESTRICTED_CASH).values())
                .containsEntry("2025", valueFormat.format(5991000000L))
                .containsEntry("2024", valueFormat.format(-794000000L))
                .containsEntry("2023", valueFormat.format(5760000000L))
                .containsEntry("2022", valueFormat.format(-10952000000L));
        assertThat(row(statements, FREE_CASH_FLOW).values())
                .containsEntry("2025", valueFormat.format(98767000000L))
                .containsEntry("2024", valueFormat.format(108807000000L))
                .containsEntry("2023", valueFormat.format(99584000000L))
                .containsEntry("2022", valueFormat.format(111443000000L));
    }

    @Test
    void missingOperatingCashFlowOrCapexMakesFreeCashFlowUnavailable() throws Exception {
        for (CashFlowStatementMetricEnumType missingMetric : List.of(OPERATING_CASH_FLOW, CAPITAL_EXPENDITURES)) {
            JsonNode companyFacts = readFixtureJson("AAPL-company-facts-pretty.json");
            ((ObjectNode) companyFacts.path("facts").path("us-gaap"))
                    .remove(missingMetric.getUsGAAPConcepts().getFirst());

            FinancialStatements statements = extract(companyFacts);

            assertThat(row(statements, missingMetric).values()).containsEntry("2025", null);
            assertThat(row(statements, FREE_CASH_FLOW).values()).containsEntry("2025", null);
            JsonNode output = objectMapper.readTree(objectMapper.writeValueAsString(row(statements, FREE_CASH_FLOW)));
            assertThat(output.path("values").has("2025")).isTrue();
            assertThat(output.path("values").path("2025").isNull()).isTrue();
        }
    }

    @Test
    void zeroCapexIsPreservedAndAllowsFreeCashFlowCalculation() throws Exception {
        JsonNode companyFacts = readFixtureJson("AAPL-company-facts-pretty.json");
        factFor2025(companyFacts, CAPITAL_EXPENDITURES).put("val", 0);

        FinancialStatements statements = extract(companyFacts);

        assertThat(row(statements, CAPITAL_EXPENDITURES).values()).containsEntry("2025", "0");
        assertThat(row(statements, FREE_CASH_FLOW).values()).containsEntry("2025", "111,482,000,000");
    }

    @Test
    void ignoresQuarterlyWrongAccessionAndWrongUnitFacts() throws Exception {
        JsonNode companyFacts = readFixtureJson("AAPL-company-facts-pretty.json");
        ObjectNode annual = factFor2025(companyFacts, OPERATING_CASH_FLOW).deepCopy();
        ArrayNode facts = factsFor(companyFacts, OPERATING_CASH_FLOW);
        facts.add(annual.deepCopy().put("start", "2025-06-29").put("val", 999));
        facts.add(annual.deepCopy().put("accn", "another-accession").put("filed", "2026-01-01").put("val", 888));
        facts.add(annual.deepCopy().put("fp", "Q4").put("filed", "2026-01-01").put("val", 777));
        ObjectNode units = (ObjectNode) companyFacts.path("facts").path("us-gaap")
                .path(OPERATING_CASH_FLOW.getUsGAAPConcepts().getFirst()).path("units");
        units.putArray("EUR").add(annual.deepCopy().put("val", 666));

        assertThat(row(extract(companyFacts), OPERATING_CASH_FLOW).values())
                .containsEntry("2025", "111,482,000,000");
    }

    @Test
    void priorPeriodWithinSameAccessionDoesNotFillCurrentYear() throws Exception {
        JsonNode companyFacts = readFixtureJson("AAPL-company-facts-pretty.json");
        factFor2025(companyFacts, OPERATING_CASH_FLOW)
                .put("start", "2023-10-01").put("end", "2024-09-28");

        FinancialStatements statements = extract(companyFacts);

        assertThat(row(statements, OPERATING_CASH_FLOW).values())
                .containsEntry("2025", null)
                .containsEntry("2024", "118,254,000,000");
        assertThat(row(statements, FREE_CASH_FLOW).values()).containsEntry("2025", null);
    }

    @Test
    void freeCashFlowRequiresMatchingStartDates() throws Exception {
        JsonNode companyFacts = readFixtureJson("AAPL-company-facts-pretty.json");
        factFor2025(companyFacts, CAPITAL_EXPENDITURES).put("start", "2024-10-01");

        FinancialStatements statements = extract(companyFacts);

        assertThat(row(statements, CAPITAL_EXPENDITURES).values()).containsEntry("2025", "12,715,000,000");
        assertThat(row(statements, FREE_CASH_FLOW).values()).containsEntry("2025", null);
    }

    @Test
    void missingOrInvalidStartDateCannotBeUsedAsAnnualCashFlow() throws Exception {
        for (String startDate : List.of("", "not-a-date")) {
            JsonNode companyFacts = readFixtureJson("AAPL-company-facts-pretty.json");
            factFor2025(companyFacts, OPERATING_CASH_FLOW).put("start", startDate);

            assertThat(row(extract(companyFacts), OPERATING_CASH_FLOW).values()).containsEntry("2025", null);
        }
    }

    @Test
    void respectsRequestedNumberOfAnnualPeriods() throws Exception {
        EdgarCashFlowStatementService onePeriodService = new EdgarCashFlowStatementService(
                HttpClient.newHttpClient(), objectMapper, "ai-experiments test@example.com", 1);

        FinancialStatements statements = onePeriodService.extractAnnualCashFlowStatement(
                "aapl", readFixtureJson("AAPL-submissions.json"), readFixtureJson("AAPL-company-facts-pretty.json"));

        assertThat(statements.statements().getFirst().periods()).containsExactly("2025");
        assertThat(row(statements, OPERATING_CASH_FLOW).values()).hasSize(1);
        assertThat(statements.sourceUrl()).hasSize(1);
    }

    @Test
    void rejectsSubmissionsWithoutAnnualFilings() throws Exception {
        JsonNode companyFacts = readFixtureJson("AAPL-company-facts-pretty.json");

        assertThatThrownBy(() -> service.extractAnnualCashFlowStatement("AAPL", objectMapper.createObjectNode(), companyFacts))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No recent 10-K found in submissions data");
    }

    private FinancialStatements extract(JsonNode companyFacts) throws Exception {
        return service.extractAnnualCashFlowStatement("aapl", readFixtureJson("AAPL-submissions.json"), companyFacts);
    }

    private ArrayNode factsFor(JsonNode companyFacts, CashFlowStatementMetricEnumType metric) {
        return (ArrayNode) companyFacts.path("facts").path("us-gaap")
                .path(metric.getUsGAAPConcepts().getFirst()).path("units").path("USD");
    }

    private ObjectNode factFor2025(JsonNode companyFacts, CashFlowStatementMetricEnumType metric) {
        for (JsonNode fact : factsFor(companyFacts, metric)) {
            if (ACCESSION_2025.equals(fact.path("accn").asText())
                    && "2025-09-27".equals(fact.path("end").asText())) {
                return (ObjectNode) fact;
            }
        }
        throw new IllegalStateException("Missing 2025 fixture fact for " + metric);
    }

    private FinancialStatements.StatementRow row(FinancialStatements statements, CashFlowStatementMetricEnumType metric) {
        return statements.statements().getFirst().rows().stream()
                .filter(row -> row.metric().equals(metric.getLabel()))
                .findFirst()
                .orElseThrow();
    }

    private JsonNode readFixtureJson(String name) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/fixtures/" + name)) {
            assertThat(input).as("fixture /fixtures/%s should be on the test classpath", name).isNotNull();
            return objectMapper.readTree(input);
        }
    }
}
