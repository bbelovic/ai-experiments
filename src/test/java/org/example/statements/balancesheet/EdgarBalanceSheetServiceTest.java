package org.example.statements.balancesheet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.statements.EdgarStatement;
import org.example.statements.StatementRow;
import org.junit.jupiter.api.Test;

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

        EdgarStatement actualStatements = localService.extractAnnualBalanceSheetStatement(
                "aapl",
                submissions,
                companyFacts
        );

        assertThat(actualStatements.ticker()).isEqualTo("AAPL");
        assertThat(actualStatements.source()).isEqualTo("EDGAR");
        assertThat(actualStatements.statements()).hasSize(1);
        assertThat(actualStatements.statements().getFirst().rows()).isNotEmpty();

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

    private static String format(Long valueToFormat) {
        return valueToFormat == null ? null : VALUE_FORMAT.format(valueToFormat);
    }

    @Test
    void extractsStatementShapedBalanceSheetFromRecent10Ks() throws Exception {
        EdgarStatement statements = getStatementsFrom10K();

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
        EdgarStatement statements = getStatementsFrom10K();
        StatementRow totalEquity = row(statements, TOTAL_EQUITY);
        String totalEquityJson = objectMapper.writeValueAsString(totalEquity);
        JsonNode jsonNode = objectMapper.readTree(totalEquityJson).path("values");
        assertThat(jsonNode.has("2025")).isTrue();
        assertThat(jsonNode.path("2025").asText()).isEqualTo("0");
        assertThat(jsonNode.has("2024")).isTrue();
        assertThat(jsonNode.path("2024").isNull()).isTrue();
    }

    private EdgarStatement getStatementsFrom10K() throws JsonProcessingException {
        EdgarStatement statements = service.extractAnnualBalanceSheetStatement(
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

    private StatementRow row(EdgarStatement statements, BalanceSheetMetricEnumType metricType) {
        return statements.statements().getFirst().rows().stream()
                .filter(row -> row.metricType() == metricType)
                .findFirst()
                .orElseThrow();
    }

    private StatementRow row(EdgarStatement statements, String label) {
        return statements.statements().getFirst().rows().stream()
                .filter(row -> row.metricType().getLabel().equals(label))
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
