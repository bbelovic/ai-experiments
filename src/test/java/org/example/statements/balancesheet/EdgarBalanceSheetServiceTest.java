package org.example.statements.balancesheet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;

class EdgarBalanceSheetServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EdgarBalanceSheetService service = new EdgarBalanceSheetService(
            HttpClient.newHttpClient(),
            objectMapper,
            "ai-experiments test@example.com"
    );

    @Test
    void extractsMappedMetricsFromLatest10KAccessionOnly() throws Exception {
        BalanceSheetSnapshot snapshot = service.extractLatestAnnualBalanceSheet(
                "aapl",
                objectMapper.readTree("""
                        {
                          "name": "Apple Inc.",
                          "filings": {
                            "recent": {
                              "form": ["10-Q", "10-K", "10-K"],
                              "accessionNumber": [
                                "0000320193-26-000011",
                                "0000320193-25-000079",
                                "0000320193-24-000123"
                              ],
                              "filingDate": ["2026-05-01", "2025-10-31", "2024-11-01"],
                              "reportDate": ["2026-03-28", "2025-09-27", "2024-09-28"],
                              "fy": ["2026", "2025", "2024"]
                            }
                          }
                        }
                        """),
                objectMapper.readTree("""
                        {
                          "cik": 320193,
                          "entityName": "Apple Inc.",
                          "facts": {
                            "us-gaap": {
                              "Assets": {
                                "units": {
                                  "USD": [
                                    {
                                      "accn": "0000320193-24-000123",
                                      "form": "10-K",
                                      "filed": "2024-11-01",
                                      "end": "2024-09-28",
                                      "val": 100
                                    },
                                    {
                                      "accn": "0000320193-25-000079",
                                      "form": "10-K",
                                      "filed": "2025-10-31",
                                      "end": "2025-09-27",
                                      "val": 33992000
                                    }
                                  ]
                                }
                              },
                              "AssetsCurrent": {
                                "units": {
                                  "USD": [
                                    {
                                      "accn": "0000320193-25-000079",
                                      "form": "10-K",
                                      "filed": "2025-10-31",
                                      "end": "2025-09-27",
                                      "val": 16925000
                                    }
                                  ]
                                }
                              },
                              "CashAndCashEquivalentsAtCarryingValue": {
                                "units": {
                                  "USD": [
                                    {
                                      "accn": "0000320193-25-000079",
                                      "form": "10-K",
                                      "filed": "2025-10-31",
                                      "end": "2025-09-27",
                                      "val": 2845000
                                    }
                                  ]
                                }
                              },
                              "ShortTermInvestments": {
                                "units": {
                                  "USD": [
                                    {
                                      "accn": "0000320193-25-000079",
                                      "form": "10-K",
                                      "filed": "2025-10-31",
                                      "end": "2025-09-27",
                                      "val": 764000
                                    }
                                  ]
                                }
                              },
                              "Liabilities": {
                                "units": {
                                  "USD": [
                                    {
                                      "accn": "0000320193-25-000079",
                                      "form": "10-K",
                                      "filed": "2025-10-31",
                                      "end": "2025-09-27",
                                      "val": 20584000
                                    }
                                  ]
                                }
                              },
                              "LiabilitiesCurrent": {
                                "units": {
                                  "USD": [
                                    {
                                      "accn": "0000320193-25-000079",
                                      "form": "10-K",
                                      "filed": "2025-10-31",
                                      "end": "2025-09-27",
                                      "val": 9610000
                                    }
                                  ]
                                }
                              },
                              "ContractWithCustomerLiabilityCurrent": {
                                "units": {
                                  "USD": [
                                    {
                                      "accn": "0000320193-25-000079",
                                      "form": "10-K",
                                      "filed": "2025-10-31",
                                      "end": "2025-09-27",
                                      "val": 1606000
                                    }
                                  ]
                                }
                              },
                              "ContractWithCustomerLiabilityNoncurrent": {
                                "units": {
                                  "USD": [
                                    {
                                      "accn": "0000320193-25-000079",
                                      "form": "10-K",
                                      "filed": "2025-10-31",
                                      "end": "2025-09-27",
                                      "val": 1054000
                                    }
                                  ]
                                }
                              },
                              "StockholdersEquity": {
                                "units": {
                                  "USD": [
                                    {
                                      "accn": "0000320193-25-000079",
                                      "form": "10-K",
                                      "filed": "2025-10-31",
                                      "end": "2025-09-27",
                                      "val": 12349000
                                    }
                                  ]
                                }
                              }
                            }
                          }
                        }
                        """)
        );

        assertThat(snapshot.ticker()).isEqualTo("AAPL");
        assertThat(snapshot.form()).isEqualTo("10-K");
        assertThat(snapshot.fiscalYear()).isEqualTo("2025");
        assertThat(snapshot.accessionNumber()).isEqualTo("0000320193-25-000079");

        assertThat(snapshot.metrics())
                .extracting(BalanceSheetMetric::key)
                .contains(
                        "total_assets",
                        "current_assets",
                        "cash_and_short_term_investments",
                        "non_current_assets",
                        "total_liabilities",
                        "current_liabilities",
                        "non_current_liabilities",
                        "current_deferred_revenue",
                        "non_current_deferred_revenue",
                        "total_equity"
                );
        assertThat(metric(snapshot, "total_assets").value()).isEqualByComparingTo(new BigDecimal("33992000"));
        assertThat(metric(snapshot, "cash_and_short_term_investments").usGaapConcept())
                .isEqualTo("derived:cash_and_cash_equivalents+short_term_investments");
        assertThat(metric(snapshot, "cash_and_short_term_investments").value()).isEqualByComparingTo(new BigDecimal("3609000"));
        assertThat(metric(snapshot, "non_current_assets").value()).isEqualByComparingTo(new BigDecimal("17067000"));
        assertThat(metric(snapshot, "non_current_liabilities").value()).isEqualByComparingTo(new BigDecimal("10974000"));
        assertThat(metric(snapshot, "current_deferred_revenue").label()).isEqualTo("Deferred revenue");
        assertThat(metric(snapshot, "non_current_deferred_revenue").label()).isEqualTo("Deferred revenue");
        assertThat(metric(snapshot, "non_current_deferred_revenue").value()).isEqualByComparingTo(new BigDecimal("1054000"));
    }

    @Test
    void extractsStatementShapedBalanceSheetFromRecent10Ks() throws Exception {
        EdgarFinancialStatements statements = service.extractAnnualBalanceSheetStatement(
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

    private BalanceSheetMetric metric(BalanceSheetSnapshot snapshot, String key) {
        return snapshot.metrics().stream()
                .filter(metric -> metric.key().equals(key))
                .findFirst()
                .orElseThrow();
    }

    private EdgarFinancialStatements.StatementRow row(EdgarFinancialStatements statements, String metric) {
        return statements.statements().getFirst().rows().stream()
                .filter(row -> row.metric().equals(metric))
                .findFirst()
                .orElseThrow();
    }
}
