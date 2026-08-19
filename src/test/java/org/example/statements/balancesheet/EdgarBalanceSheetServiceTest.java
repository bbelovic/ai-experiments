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

    private BalanceSheetMetric metric(BalanceSheetSnapshot snapshot, String key) {
        return snapshot.metrics().stream()
                .filter(metric -> metric.key().equals(key))
                .findFirst()
                .orElseThrow();
    }
}
