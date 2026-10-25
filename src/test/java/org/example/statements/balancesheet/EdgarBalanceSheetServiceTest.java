package org.example.statements.balancesheet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.statements.FinancialStatements;
import org.example.statements.FinancialStatements.StatementRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.example.statements.balancesheet.BalanceSheetMetricEnumType.*;
import static org.example.statements.balancesheet.EdgarBalanceSheetService.VALUE_FORMAT;

class EdgarBalanceSheetServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EdgarBalanceSheetService service = new EdgarBalanceSheetService(
            HttpClient.newHttpClient(),
            objectMapper,
            "ai-experiments test@example.com",
            4,
            EnumSet.allOf(BalanceSheetMetricEnumType.class)
    );

    @Test
    void extractsStatementShapedBalanceSheetFromRealAAPL10Ks() throws Exception {
        JsonNode submissions = readFixtureJson("AAPL-submissions.json");
        JsonNode companyFacts = readFixtureJson("AAPL-company-facts-pretty.json");

        var localService = new EdgarBalanceSheetService(
                HttpClient.newHttpClient(),
                objectMapper,
                "ai-experiments test@example.com",
                1,
                EnumSet.allOf(BalanceSheetMetricEnumType.class)
        );

        FinancialStatements actualStatements = localService.extractAnnualBalanceSheetStatement(
                "aapl",
                submissions,
                companyFacts
        );

        assertThat(actualStatements.ticker()).isEqualTo("AAPL");
        assertThat(actualStatements.source()).isEqualTo("EDGAR");
        assertThat(actualStatements.statements()).hasSize(1);
        assertThat(actualStatements.statements().getFirst().rows()).isNotEmpty();
        assertThat(actualStatements.statements().getFirst().rows())
                .extracting(StatementRow::metric)
                .doesNotHaveDuplicates()
                .contains("Current deferred revenue", "Non-current deferred revenue");

        Map<BalanceSheetMetricEnumType, Long> expectedMetricsAndValues = expectedBalanceSheetMetrics();
        expectedMetricsAndValues.forEach((metricDefinition, expectedValue) -> {
                    assertThat(row(actualStatements, metricDefinition).values())
                            .as("Expecting metric [%s] to have value [%s]", metricDefinition, format(expectedValue))
                            .containsEntry("2025", format(expectedValue));
                });

        List.of("AccruedIncomeTaxesNoncurrent", "AccruedIncomeTaxesCurrent", "AccruedLiabilities")
                .forEach(notPresent -> {
                    assertThatThrownBy(() -> row(actualStatements, notPresent))
                            .isInstanceOf(NoSuchElementException.class)
                            .hasMessage("No value present");
                });
    }

    private static Map<BalanceSheetMetricEnumType, Long> expectedBalanceSheetMetrics() {
        Map<BalanceSheetMetricEnumType, Long> expectedMetricsAndValues = new EnumMap<>(BalanceSheetMetricEnumType.class);
        expectedMetricsAndValues.put(TOTAL_ASSETS, 359241000000L);
        expectedMetricsAndValues.put(CURRENT_ASSETS, 147957000000L);
        expectedMetricsAndValues.put(CASH_AND_CASH_EQUIVALENTS, 35934000000L);
        expectedMetricsAndValues.put(SHORT_TERM_INVESTMENTS, 18763000000L);
        expectedMetricsAndValues.put(CASH_AND_SHORT_TERM_INVESTMENTS, (35934000000L + 18763000000L));
        expectedMetricsAndValues.put(RECEIVABLES, 39777000000L);
        expectedMetricsAndValues.put(INVENTORY, 5718000000L);
        expectedMetricsAndValues.put(OTHER_CURRENT_ASSETS, (expectedMetricsAndValues.get(CURRENT_ASSETS) -
                        (expectedMetricsAndValues.get(CASH_AND_CASH_EQUIVALENTS) + expectedMetricsAndValues.get(SHORT_TERM_INVESTMENTS) +
                expectedMetricsAndValues.get(RECEIVABLES) + expectedMetricsAndValues.get(INVENTORY)) ));
        expectedMetricsAndValues.put(NON_CURRENT_ASSETS, (expectedMetricsAndValues.get(TOTAL_ASSETS) - expectedMetricsAndValues.get(CURRENT_ASSETS)));
        expectedMetricsAndValues.put(PPE, 49834000000L);
        expectedMetricsAndValues.put(GOODWILL, null);
        expectedMetricsAndValues.put(INTANGIBLE_ASSETS, null);
        expectedMetricsAndValues.put(LONG_TERM_INVESTMENTS, 77723000000L);
        expectedMetricsAndValues.put(TAX_ASSETS, 27451000000L);
        expectedMetricsAndValues.put(OTHER_NON_CURRENT_ASSETS, 83727000000L);
        expectedMetricsAndValues.put(TOTAL_LIABILITIES, 285508000000L);
        expectedMetricsAndValues.put(CURRENT_LIABILITIES, 165631000000L);
        expectedMetricsAndValues.put(ACCOUNTS_PAYABLE, 69860000000L);
        expectedMetricsAndValues.put(SHORT_TERM_DEBT, 12350000000L);
        expectedMetricsAndValues.put(TAX_PAYABLES, null);
        expectedMetricsAndValues.put(CURRENT_DEFERRED_REVENUE, 9055000000L);
        expectedMetricsAndValues.put(NON_CURRENT_LIABILITIES, 119877000000L);
        expectedMetricsAndValues.put(LONG_TERM_DEBT, 78328000000L);
        expectedMetricsAndValues.put(NON_CURRENT_DEFERRED_REVENUE, null);
        expectedMetricsAndValues.put(DEFERRED_TAX, null);
        expectedMetricsAndValues.put(OTHER_CURRENT_LIABILITIES, null);
        expectedMetricsAndValues.put(OTHER_NON_CURRENT_LIABILITIES, 41549000000L);
        expectedMetricsAndValues.put(TOTAL_EQUITY, 73733000000L);
        expectedMetricsAndValues.put(PREFERRED_STOCK, null);
        expectedMetricsAndValues.put(COMMON_STOCK, 93568000000L);
        expectedMetricsAndValues.put(RETAINED_EARNINGS, -14264000000L);
        expectedMetricsAndValues.put(AOCI, -5571000000L);
        expectedMetricsAndValues.put(OTHER_EQUITY, null);
        return expectedMetricsAndValues;
    }

    private static String format(Long valueToFormat) {
        return valueToFormat == null ? null : VALUE_FORMAT.format(valueToFormat);
    }

    @Test
    void extractsStatementShapedBalanceSheetFromRecent10Ks() throws Exception {
        FinancialStatements statements = getStatementsFrom10K();

        assertThat(statements.ticker()).isEqualTo("MO");
        assertThat(statements.source()).isEqualTo("EDGAR");
        assertThat(statements.sourceUrl()).containsExactly(
                "https://www.sec.gov/Archives/edgar/data/764180/000076418026000010/mo-20251231.htm",
                "https://www.sec.gov/Archives/edgar/data/764180/000076418025000010/mo-20241231.htm",
                "https://www.sec.gov/Archives/edgar/data/764180/000076418024000010/mo-20231231.htm",
                "https://www.sec.gov/Archives/edgar/data/764180/000076418023000010/mo-20221231.htm");
        assertThat(statements.statements()).hasSize(1);
        assertThat(statements.statements().getFirst().name()).isEqualTo("Balance Sheet");
        assertThat(statements.statements().getFirst().periods()).containsExactly("2025", "2024", "2023", "2022");

        assertThat(row(statements, TOTAL_ASSETS).values())
                .containsEntry("2025", "35,017,000")
                .containsEntry("2024", "35,177,000")
                .containsEntry("2023", "38,570,000")
                .containsEntry("2022", "36,954,000");
        assertThat(row(statements, CASH_AND_SHORT_TERM_INVESTMENTS).values())
                .containsEntry("2025", "4,493,000")
                .containsEntry("2024", null)
                .containsEntry("2023", null)
                .containsEntry("2022", null);
        assertThat(row(statements, OTHER_CURRENT_ASSETS).values())
                .containsEntry("2025", "-282,000")
                .containsEntry("2024", null)
                .containsEntry("2023", null)
                .containsEntry("2022", null);
        assertThat(row(statements, TOTAL_EQUITY).values())
                .containsEntry("2025", "0")
                .containsEntry("2024", null)
                .containsEntry("2023", null)
                .containsEntry("2022", null);
    }

    @Test
    void missingFactIsSerializedAsNull() throws Exception {
        FinancialStatements statements = getStatementsFrom10K();
        StatementRow totalEquity = row(statements, TOTAL_EQUITY);
        String totalEquityJson = objectMapper.writeValueAsString(totalEquity);
        assertThat(objectMapper.readTree(totalEquityJson).path("metric").asText()).isEqualTo(TOTAL_EQUITY.getLabel());
        assertThat(objectMapper.readTree(totalEquityJson).has("metricType")).isFalse();
        JsonNode jsonNode = objectMapper.readTree(totalEquityJson).path("values");
        assertThat(jsonNode.has("2025")).isTrue();
        assertThat(jsonNode.path("2025").asText()).isEqualTo("0");
        assertThat(jsonNode.has("2024")).isTrue();
        assertThat(jsonNode.path("2024").isNull()).isTrue();
    }

    private FinancialStatements getStatementsFrom10K() throws JsonProcessingException {
        FinancialStatements statements = service.extractAnnualBalanceSheetStatement(
                "mo",
                objectMapper.readTree("""
                        {
                          "name": "Altria Group, Inc.",
                          "filings": {
                            "recent": {
                              "form": ["10-Q", "10-K", "10-K", "10-K", "10-K", "10-K"],
                              "accessionNumber": [
                                "0000764180-26-000011",
                                "0000764180-26-000010",
                                "0000764180-25-000010",
                                "0000764180-24-000010",
                                "0000764180-23-000010",
                                "0000764180-22-000010"
                              ],
                              "filingDate": [
                                "2026-04-25",
                                "2026-02-14",
                                "2025-02-14",
                                "2024-02-14",
                                "2023-02-14",
                                "2022-02-14"
                              ],
                              "reportDate": [
                                "2026-03-31",
                                "2025-12-31",
                                "2024-12-31",
                                "2023-12-31",
                                "2022-12-31",
                                "2021-12-31"
                              ],
                              "fy": ["2026", "2025", "2024", "2023", "2022", "2021"],
                              "primaryDocument": [
                                "mo-20260331.htm",
                                "mo-20251231.htm",
                                "mo-20241231.htm",
                                "mo-20231231.htm",
                                "mo-20221231.htm",
                                "mo-20211231.htm"
                              ]
                            }
                          }
                        }
                        """),
                objectMapper.readTree("""
                        {
                          "cik": 764180,
                          "entityName": "Altria Group, Inc.",
                          "facts": {
                            "us-gaap": {
                              "Assets": {
                                "units": {
                                  "USD": [
                                    {"accn": "0000764180-26-000010", "filed": "2026-02-14", "end": "2025-12-31", "val": 35017000},
                                    {"accn": "0000764180-25-000010", "filed": "2025-02-14", "end": "2024-12-31", "val": 35177000},
                                    {"accn": "0000764180-24-000010", "filed": "2024-02-14", "end": "2023-12-31", "val": 38570000},
                                    {"accn": "0000764180-23-000010", "filed": "2023-02-14", "end": "2022-12-31", "val": 36954000}
                                  ]
                                }
                              },
                              "AssetsCurrent": {
                                "units": {
                                  "USD": [
                                    {"accn": "0000764180-26-000010", "filed": "2026-02-14", "end": "2025-12-31", "val": 5544000},
                                    {"accn": "0000764180-25-000010", "filed": "2025-02-14", "end": "2024-12-31", "val": 4513000}
                                  ]
                                }
                              },
                              "CashAndCashEquivalentsAtCarryingValue": {
                                "units": {
                                  "USD": [
                                    {"accn": "0000764180-26-000010", "filed": "2026-02-14", "end": "2025-12-31", "val": 4481000},
                                    {"accn": "0000764180-25-000010", "filed": "2025-02-14", "end": "2024-12-31", "val": 3127000}
                                  ]
                                }
                              },
                              "ShortTermInvestments": {
                                "units": {
                                  "USD": [
                                    {"accn": "0000764180-26-000010", "filed": "2026-02-14", "end": "2025-12-31", "val": 12000}
                                  ]
                                }
                              },
                              "AccountsReceivableNetCurrent": {
                                "units": {
                                  "USD": [
                                    {"accn": "0000764180-26-000010", "filed": "2026-02-14", "end": "2025-12-31", "val": 263000}
                                  ]
                                }
                              },
                              "InventoryNet": {
                                "units": {
                                  "USD": [
                                    {"accn": "0000764180-26-000010", "filed": "2026-02-14", "end": "2025-12-31", "val": 1070000}
                                  ]
                                }
                              },
                              "Liabilities": {
                                "units": {
                                  "USD": [
                                    {"accn": "0000764180-26-000010", "filed": "2026-02-14", "end": "2025-12-31", "val": 38469000}
                                  ]
                                }
                              },
                              "LiabilitiesCurrent": {
                                "units": {
                                  "USD": [
                                    {"accn": "0000764180-26-000010", "filed": "2026-02-14", "end": "2025-12-31", "val": 9154000}
                                  ]
                                }
                              },
                              "StockholdersEquity": {
                                "units": {
                                  "USD": [
                                    {"accn": "0000764180-26-000010", "filed": "2026-02-14", "end": "2025-12-31", "val": 0}
                                  ]
                                }
                              }
                            }
                          }
                        }
                        """)
        );
        return statements;
    }

    private StatementRow row(FinancialStatements statements, BalanceSheetMetricEnumType metricType) {
        return row(statements, metricType.getLabel());
    }

    private StatementRow row(FinancialStatements statements, String label) {
        return statements.statements().getFirst().rows().stream()
                .filter(row -> row.metric().equals(label))
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

    /**
     * Rejects the first investment concept's prior-period fact and tries the next concept.
     * These abbreviated USD fact arrays all belong to the selected accession; the target
     * report date is 2025-12-31:
     * <pre>{@code
     * {
     *   "CashAndCashEquivalentsAtCarryingValue": [{"end":"2025-12-31","val":100}],
     *   "ShortTermInvestments": [{"end":"2024-12-31","val":20}],
     *   "MarketableSecuritiesCurrent": [{"end":"2025-12-31","val":30}]
     * }
     * }</pre>
     * Expects investments of 30 and combined cash and investments of 130, using only
     * inputs for the target date.
     */
    @Test
    void skipsWrongDateConceptAndDerivesFromExactDateFallback() {
        ObjectNode facts = syntheticFacts();
        addFact(facts, "CashAndCashEquivalentsAtCarryingValue", "2025-12-31", 100);
        addFact(facts, "ShortTermInvestments", "2024-12-31", 20);
        addFact(facts, "MarketableSecuritiesCurrent", "2025-12-31", 30);

        FinancialStatements result = extractSynthetic("2025-12-31", facts);

        assertThat(row(result, SHORT_TERM_INVESTMENTS).values()).containsEntry("2025", "30");
        assertThat(row(result, CASH_AND_SHORT_TERM_INVESTMENTS).values()).containsEntry("2025", "130");
    }

    /**
     * Verifies that an ineligible investment fact leaves both the direct value and its
     * dependent sum unavailable. Example USD fact excerpts for target 2025-12-31:
     * <pre>{@code
     * {
     *   "CashAndCashEquivalentsAtCarryingValue": [{"end":"2025-12-31","val":100}],
     *   "ShortTermInvestments": [{"end":"2024-12-31","val":20}]
     * }
     * }</pre>
     * Also tries a future, empty, and malformed end date. Each investment row and combined
     * row must serialize with {@code {"values":{"2025":null}}}, preserving the year key.
     *
     * @param wrongDate the ineligible investment end date
     */
    @ParameterizedTest
    @ValueSource(strings = {"2024-12-31", "2026-12-31", "", "invalid"})
    void wrongDateFactsRemainMissingIncludingDerivedOutput(String wrongDate) throws Exception {
        ObjectNode facts = syntheticFacts();
        addFact(facts, "CashAndCashEquivalentsAtCarryingValue", "2025-12-31", 100);
        addFact(facts, "ShortTermInvestments", wrongDate, 20);

        FinancialStatements result = extractSynthetic("2025-12-31", facts);

        for (BalanceSheetMetricEnumType metric : List.of(SHORT_TERM_INVESTMENTS, CASH_AND_SHORT_TERM_INVESTMENTS)) {
            JsonNode output = objectMapper.valueToTree(row(result, metric));
            assertThat(output.path("values").has("2025")).isTrue();
            assertThat(output.path("values").path("2025").isNull()).isTrue();
        }
    }

    /**
     * Keeps the filing-date tie-breaker within exact reporting-date matches. Example
     * Assets USD facts for the selected accession and target 2025-12-31:
     * <pre>{@code
     * [
     *   {"end":"2025-12-31","filed":"2026-03-01","val":110},
     *   {"end":"2025-12-31","filed":"2026-02-01","val":100},
     *   {"end":"2026-12-31","filed":"2027-02-01","val":999},
     *   {"end":"2024-12-31","filed":"2027-03-01","val":888}
     * ]
     * }</pre>
     * Expects 110 even though it appears first and the wrong-period facts were filed later.
     */
    @Test
    void selectsLatestFiledExactMatchRegardlessOfArrayOrder() {
        ObjectNode facts = syntheticFacts();
        addFact(facts, "Assets", "2025-12-31", 110).put("filed", "2026-03-01");
        addFact(facts, "Assets", "2025-12-31", 100).put("filed", "2026-02-01");
        addFact(facts, "Assets", "2026-12-31", 999).put("filed", "2027-02-01");
        addFact(facts, "Assets", "2024-12-31", 888).put("filed", "2027-03-01");

        assertThat(row(extractSynthetic("2025-12-31", facts), TOTAL_ASSETS).values())
                .containsEntry("2025", "110");
    }

    /**
     * Ensures exact date matching still enforces accession, numeric value, and USD unit.
     * Example Assets units; all facts have end date 2025-12-31:
     * <pre>{@code
     * {
     *   "USD": [
     *     {"accn":"another-accession","end":"2025-12-31","val":999},
     *     {"accn":"selected-accession","end":"2025-12-31","val":"888"}
     *   ],
     *   "EUR": [{"accn":"selected-accession","end":"2025-12-31","val":777}]
     * }
     * }</pre>
     * Initially expects null. Adding a numeric USD fact with value 0 for the selected
     * accession must then produce the string "0", distinguishing a reported zero from missing data.
     */
    @Test
    void retainsAccessionNumericAndUsdConstraints() {
        ObjectNode facts = syntheticFacts();
        addFact(facts, "Assets", "2025-12-31", 999).put("accn", "another-accession");
        addFact(facts, "Assets", "2025-12-31", 888).put("val", "888");
        ObjectNode units = (ObjectNode) facts.path("facts").path("us-gaap").path("Assets").path("units");
        units.putArray("EUR").addObject().put("accn", "selected-accession")
                .put("end", "2025-12-31").put("val", 777);

        assertThat(row(extractSynthetic("2025-12-31", facts), TOTAL_ASSETS).values())
                .containsEntry("2025", null);
        addFact(facts, "Assets", "2025-12-31", 0);
        assertThat(row(extractSynthetic("2025-12-31", facts), TOTAL_ASSETS).values())
                .containsEntry("2025", "0");
    }

    /**
     * Reads each filing's own non-December snapshot date. Example submissions recent metadata:
     * <pre>{@code
     * {
     *   "accessionNumber":["selected-accession","previous-accession"],
     *   "filingDate":["2025-10-31","2024-11-01"],
     *   "reportDate":["2025-09-27","2024-09-28"],
     *   "fy":["2025","2024"]
     * }
     * }</pre>
     * Assets are 100 and 90 on those respective report dates. Competing current-accession
     * facts have value 999 on the previous report date, its calendar-year anniversary,
     * the filing date, and December 31. Expects {@code {"2025":"100","2024":"90"}},
     * proving that each year's report date is read independently rather than inferred.
     */
    @Test
    void usesEachNonDecemberReportDateIndependently() throws Exception {
        ObjectNode submissions = syntheticSubmissions("2025-09-27");
        ObjectNode recent = (ObjectNode) submissions.path("filings").path("recent");
        ((ArrayNode) recent.path("filingDate")).set(0, objectMapper.getNodeFactory().textNode("2025-10-31"));
        recent.withArray("form").add("10-K");
        recent.withArray("accessionNumber").add("previous-accession");
        recent.withArray("filingDate").add("2024-11-01");
        recent.withArray("reportDate").add("2024-09-28");
        recent.withArray("fy").add("2024");
        ObjectNode facts = syntheticFacts();
        addFact(facts, "Assets", "2025-09-27", 100);
        for (String wrongDate : List.of("2024-09-28", "2025-09-28", "2025-10-31", "2025-12-31")) {
            addFact(facts, "Assets", wrongDate, 999);
        }
        addFact(facts, "Assets", "2024-09-28", 90).put("accn", "previous-accession");

        FinancialStatements result = service.extractAnnualBalanceSheetStatement("TEST", submissions, facts);

        assertThat(row(result, TOTAL_ASSETS).values()).containsExactly(
                Map.entry("2025", "100"), Map.entry("2024", "90"));
    }

    /**
     * Uses the reporting date when the 10-K was submitted in the following calendar year.
     * Example metadata and abbreviated Assets USD facts for the same accession:
     * <pre>{@code
     * {
     *   "reportDate":"2025-12-31","filingDate":"2026-02-14","fy":"2025",
     *   "Assets":[{"end":"2026-02-14","val":999},{"end":"2025-12-31","val":100}]
     * }
     * }</pre>
     * Expects 100 in the 2025 column; the filing-date fact must not supply that column.
     */
    @Test
    void usesReportDateWhenFilingWasSubmittedInTheNextCalendarYear() {
        ObjectNode facts = syntheticFacts();
        addFact(facts, "Assets", "2026-02-14", 999);
        addFact(facts, "Assets", "2025-12-31", 100);

        assertThat(row(extractSynthetic("2025-12-31", facts), TOTAL_ASSETS).values())
                .containsEntry("2025", "100");
    }

    /**
     * Rejects unavailable or invalid reporting metadata even if a fact repeats the same
     * invalid string. Example excerpts include an impossible calendar date and plausible alternatives:
     * <pre>{@code
     * {
     *   "reportDate":["2025-02-29"],
     *   "Assets":[
     *     {"end":"2025-02-29","val":999},
     *     {"end":"2025-12-31","val":100},
     *     {"end":"2026-02-14","val":200}
     *   ]
     * }
     * }</pre>
     * Covers null, empty, blank, malformed, impossible, and noncanonical dates. Expects an
     * IllegalArgumentException naming reportDate and the accession, without guessing from facts.
     *
     * @param reportDate the unavailable or invalid selected filing report date
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "invalid", "2025-02-29", "2025-13-01", "2025-9-27"})
    void rejectsInvalidReportDatesEvenWhenFactEndMatches(String reportDate) {
        ObjectNode facts = syntheticFacts();
        addFact(facts, "Assets", reportDate, 999);
        addFact(facts, "Assets", "2025-12-31", 100);
        addFact(facts, "Assets", "2026-02-14", 200);

        assertThatThrownBy(() -> extractSynthetic(reportDate, facts))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reportDate")
                .hasMessageContaining("selected-accession");
    }

    /**
     * Checks both a missing reportDate array entry and an absent array. Example recent
     * metadata for the first case:
     * <pre>{@code
     * {
     *   "form":["10-K"],"accessionNumber":["selected-accession"],
     *   "filingDate":["2026-02-14"],"fy":["2025"],"reportDate":[]
     * }
     * }</pre>
     * Repeats after removing reportDate entirely. Both cases must raise an explicit
     * validation error identifying the accession instead of a null dereference or guessed date.
     */
    @Test
    void rejectsMissingReportDateArrayAndMissingEntry() {
        ObjectNode submissions = syntheticSubmissions("2025-12-31");
        ObjectNode recent = (ObjectNode) submissions.path("filings").path("recent");
        recent.putArray("reportDate");
        assertInvalidReportDate(submissions);
        recent.remove("reportDate");
        assertInvalidReportDate(submissions);
    }

    private void assertInvalidReportDate(ObjectNode submissions) {
        assertThatThrownBy(() -> service.extractAnnualBalanceSheetStatement("TEST", submissions, syntheticFacts()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reportDate")
                .hasMessageContaining("selected-accession");
    }

    /**
     * Moves one calculation input to the prior period while leaving other inputs at
     * target 2025-12-31. For example, these abbreviated USD facts make current assets unavailable:
     * <pre>{@code
     * {
     *   "Assets":[{"end":"2025-12-31","val":1000}],
     *   "AssetsCurrent":[{"end":"2024-12-31","val":400}],
     *   "CashAndCashEquivalentsAtCarryingValue":[{"end":"2025-12-31","val":100}],
     *   "ShortTermInvestments":[{"end":"2025-12-31","val":30}],
     *   "AccountsReceivableNetCurrent":[{"end":"2025-12-31","val":90}],
     *   "InventoryNet":[{"end":"2025-12-31","val":70}]
     * }
     * }</pre>
     * Expects non-current assets to be null when either subtraction input is wrong-dated,
     * and other current assets to be null when its total or any component is wrong-dated.
     *
     * @param wrongDateConcept the single concept whose fact is moved to 2024-12-31
     */
    @ParameterizedTest
    @ValueSource(strings = {"Assets", "AssetsCurrent", "CashAndCashEquivalentsAtCarryingValue",
            "ShortTermInvestments", "AccountsReceivableNetCurrent", "InventoryNet"})
    void propagatesWrongDateInputsThroughSubtractionAndResiduals(String wrongDateConcept) {
        ObjectNode facts = completeCalculationFacts();
        ObjectNode fact = (ObjectNode) facts.path("facts").path("us-gaap").path(wrongDateConcept)
                .path("units").path("USD").get(0);
        fact.put("end", "2024-12-31");
        FinancialStatements result = extractSynthetic("2025-12-31", facts);

        if (List.of("Assets", "AssetsCurrent").contains(wrongDateConcept)) {
            assertThat(row(result, NON_CURRENT_ASSETS).values()).containsEntry("2025", null);
        }
        if (!wrongDateConcept.equals("Assets")) {
            assertThat(row(result, OTHER_CURRENT_ASSETS).values()).containsEntry("2025", null);
        }
    }

    /**
     * Falls through from a wrong-date reported residual to a calculation using eligible
     * inputs. Example abbreviated USD facts for target 2025-12-31:
     * <pre>{@code
     * {
     *   "Assets":[{"end":"2025-12-31","val":1000}],
     *   "AssetsCurrent":[{"end":"2025-12-31","val":400}],
     *   "CashAndCashEquivalentsAtCarryingValue":[{"end":"2025-12-31","val":100}],
     *   "ShortTermInvestments":[{"end":"2025-12-31","val":30}],
     *   "AccountsReceivableNetCurrent":[{"end":"2025-12-31","val":90}],
     *   "InventoryNet":[{"end":"2025-12-31","val":70}],
     *   "OtherCurrentAssets":[{"end":"2024-12-31","val":999}]
     * }
     * }</pre>
     * Expects non-current assets of 1000 - 400 = 600 and other current assets of
     * 400 - 100 - 30 - 90 - 70 = 110, ignoring the reported prior-period residual.
     */
    @Test
    void derivesSameDateValuesWhenReportedResidualHasWrongDate() {
        ObjectNode facts = completeCalculationFacts();
        addFact(facts, "OtherCurrentAssets", "2024-12-31", 999);

        FinancialStatements result = extractSynthetic("2025-12-31", facts);

        assertThat(row(result, NON_CURRENT_ASSETS).values()).containsEntry("2025", "600");
        assertThat(row(result, OTHER_CURRENT_ASSETS).values()).containsEntry("2025", "110");
    }

    private ObjectNode completeCalculationFacts() {
        ObjectNode facts = syntheticFacts();
        addFact(facts, "Assets", "2025-12-31", 1000);
        addFact(facts, "AssetsCurrent", "2025-12-31", 400);
        addFact(facts, "CashAndCashEquivalentsAtCarryingValue", "2025-12-31", 100);
        addFact(facts, "ShortTermInvestments", "2025-12-31", 30);
        addFact(facts, "AccountsReceivableNetCurrent", "2025-12-31", 90);
        addFact(facts, "InventoryNet", "2025-12-31", 70);
        return facts;
    }

    private FinancialStatements extractSynthetic(String reportDate, ObjectNode facts) {
        return service.extractAnnualBalanceSheetStatement("TEST", syntheticSubmissions(reportDate), facts);
    }

    private ObjectNode syntheticSubmissions(String reportDate) {
        ObjectNode submissions = objectMapper.createObjectNode();
        ObjectNode recent = submissions.putObject("filings").putObject("recent");
        recent.putArray("form").add("10-K");
        recent.putArray("accessionNumber").add("selected-accession");
        recent.putArray("filingDate").add("2026-02-14");
        recent.putArray("reportDate").add(reportDate);
        recent.putArray("fy").add("2025");
        return submissions;
    }

    private ObjectNode syntheticFacts() {
        ObjectNode facts = objectMapper.createObjectNode().put("cik", 1);
        facts.putObject("facts").putObject("us-gaap");
        return facts;
    }

    private ObjectNode addFact(ObjectNode facts, String concept, String end, long value) {
        ObjectNode gaap = (ObjectNode) facts.path("facts").path("us-gaap");
        if (!gaap.has(concept)) {
            gaap.putObject(concept).putObject("units").putArray("USD");
        }
        ArrayNode values = (ArrayNode) gaap.path(concept).path("units").path("USD");
        return values.addObject().put("accn", "selected-accession")
                .put("filed", "2026-02-14").put("end", end).put("val", value);
    }
}
