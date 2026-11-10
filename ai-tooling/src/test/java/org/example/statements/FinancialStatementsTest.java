package org.example.statements;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.statements.FinancialStatements.StatementRow;
import org.example.statements.FinancialStatements.StatementTable;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatementsTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void roundTripsMultipleStatementTypesWithIndependentPeriodsAndMissingValues() throws Exception {
        Map<String, String> revenues = new LinkedHashMap<>();
        revenues.put("2025", "466,823,000");
        revenues.put("2024", null);
        FinancialStatements statements = new FinancialStatements(
                "AAPL",
                "DIVIDEND_WATCH",
                List.of("https://dividend.watch/symbol/aapl-nasdaq/fundamentals"),
                List.of(
                        new StatementTable("Income Statement", List.of("2025", "2024"),
                                List.of(new StatementRow("Revenue", revenues))),
                        new StatementTable("Balance Sheet", List.of("2025"),
                                List.of(new StatementRow("Total assets", Map.of("2025", "359,241,000")))),
                        new StatementTable("Cash Flow Statement", List.of("2025"),
                                List.of(new StatementRow("Free cash flow", Map.of("2025", "143,482,000"))))
                )
        );

        String json = objectMapper.writeValueAsString(statements);

        assertThat(objectMapper.readTree(json)).isEqualTo(objectMapper.readTree("""
                {
                  "ticker": "AAPL",
                  "source": "DIVIDEND_WATCH",
                  "sourceUrl": ["https://dividend.watch/symbol/aapl-nasdaq/fundamentals"],
                  "statements": [
                    {
                      "name": "Income Statement",
                      "periods": ["2025", "2024"],
                      "rows": [{"metric": "Revenue", "values": {"2025": "466,823,000", "2024": null}}]
                    },
                    {
                      "name": "Balance Sheet",
                      "periods": ["2025"],
                      "rows": [{"metric": "Total assets", "values": {"2025": "359,241,000"}}]
                    },
                    {
                      "name": "Cash Flow Statement",
                      "periods": ["2025"],
                      "rows": [{"metric": "Free cash flow", "values": {"2025": "143,482,000"}}]
                    }
                  ]
                }
                """));
        FinancialStatements restored = objectMapper.readValue(json, FinancialStatements.class);
        assertThat(restored).isEqualTo(statements);
        assertThat(restored.statements().getFirst().rows().getFirst().values().keySet())
                .containsExactly("2025", "2024");
    }
}
