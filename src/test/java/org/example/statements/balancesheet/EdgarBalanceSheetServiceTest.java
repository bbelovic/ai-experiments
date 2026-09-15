package org.example.statements.balancesheet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.statements.EdgarStatement;
import org.example.statements.StatementRow;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.example.statements.balancesheet.EdgarBalanceSheetService.VALUE_FORMAT;

class EdgarBalanceSheetServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EdgarBalanceSheetService service = new EdgarBalanceSheetService(
            HttpClient.newHttpClient(),
            objectMapper,
            "ai-experiments test@example.com",
            4,
            EdgarBalanceSheetService.METRICS
    );

    @Test
    void extractsStatementShapedBalanceSheetFromRealAAPL10Ks() throws Exception {
        JsonNode submissions = readFixtureJson("AAPL-submissions.json");
        JsonNode companyFacts = readFixtureJson("AAPL-company-facts-pretty.json");

        BalanceSheetMetricDefinition apMetricDefinition = new BalanceSheetMetricDefinition("accounts_payable", "Accounts payable",
                "Liabilities", List.of("AccountsPayableCurrent"));
        BalanceSheetMetricDefinition receivablesMetricDefinition =
                new BalanceSheetMetricDefinition("receivables", "Receivables", "Assets",
                List.of("AccountsReceivableNetCurrent",
                        "AccountsReceivableNet",
                        "ReceivablesNetCurrent"));
        BalanceSheetMetricDefinition assetsMetricDefinition = new BalanceSheetMetricDefinition("total_assets", "Total assets", "Assets", List.of("Assets"));
        BalanceSheetMetricDefinition currentAssetsMetricDefinition = new BalanceSheetMetricDefinition("current_assets", "Current assets", "Assets", List.of("AssetsCurrent"));

        BalanceSheetMetricDefinition cashAndEquivalents = new BalanceSheetMetricDefinition("cash_and_cash_equivalents", "Cash & cash equivalents", "Assets",
                List.of("CashAndCashEquivalentsAtCarryingValue",
                        "CashCashEquivalentsRestrictedCashAndRestrictedCashEquivalents"));
        BalanceSheetMetricDefinition shortTermInvestments = new BalanceSheetMetricDefinition("short_term_investments", "Short-term investments", "Assets",
                List.of("ShortTermInvestments",
                        "MarketableSecuritiesCurrent"));
        BalanceSheetMetricDefinition inventory = new BalanceSheetMetricDefinition("inventory", "Inventory", "Assets", List.of("InventoryNet"));

        var localService = new EdgarBalanceSheetService(
                HttpClient.newHttpClient(),
                objectMapper,
                "ai-experiments test@example.com",
                1,
                List.of(assetsMetricDefinition, currentAssetsMetricDefinition, cashAndEquivalents, shortTermInvestments,
                new BalanceSheetMetricDefinition("cash_and_short_term_investments", "Cash & short-term investments", "Assets", List.of()), inventory)
        );

        EdgarStatement statements = localService.extractAnnualBalanceSheetStatement(
                "aapl",
                submissions,
                companyFacts
        );

        assertThat(statements.ticker()).isEqualTo("AAPL");
        assertThat(statements.source()).isEqualTo("EDGAR");
        assertThat(statements.statements()).hasSize(1);
        assertThat(statements.statements().getFirst().rows()).isNotEmpty();

        Map.of("Cash & cash equivalents", 35934000000L, "Short-term investments", 18763000000L,
                        "Cash & short-term investments", (35934000000L + 18763000000L),
                        "Total assets", 359241000000L, "Current assets", 147957000000L, "Inventory", 5718000000L)
                .forEach((label, expectedValue) -> {
                    assertThat(row(statements, label).values())
                            .containsEntry("2025", VALUE_FORMAT.format(expectedValue));
                });

        List.of("AccruedIncomeTaxesNoncurrent", "AccruedIncomeTaxesCurrent", "AccruedLiabilities")
                .forEach(notPresent -> {
                    assertThatThrownBy(() -> row(statements, notPresent))
                            .hasMessage("No value present");
                });
    }

    @Test
    void extractsStatementShapedBalanceSheetFromRecent10Ks() throws Exception {
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
                                    {"accn": "0000764180-26-000010", "filed": "2026-02-14", "end": "2025-12-31", "val": -3502000}
                                  ]
                                }
                              }
                            }
                          }
                        }
                        """)
        );

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

        assertThat(row(statements, "Total assets").values())
                .containsEntry("2025", "35,017,000")
                .containsEntry("2024", "35,177,000")
                .containsEntry("2023", "38,570,000")
                .containsEntry("2022", "36,954,000");
        assertThat(row(statements, "Cash & short-term investments").values())
                .containsEntry("2025", "4,493,000")
                .containsEntry("2024", "0");
        assertThat(row(statements, "Other current assets").values())
                .containsEntry("2025", "-282,000");
        assertThat(row(statements, "Total equity").values())
                .containsEntry("2025", "-3,502,000")
                .containsEntry("2024", "0");
    }

    private StatementRow row(EdgarStatement statements, String metric) {
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
