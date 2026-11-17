package org.example.tools;

import org.example.statements.FinancialStatements;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class EdgarDataLoaderToolTest {
    private static final FinancialStatements TEST_FIXTURE = new FinancialStatements(
            "AAPL",
            "EDGAR",
            List.of("https://edgar.com"),
            List.of(
                    new FinancialStatements.StatementTable("Income Statement", List.of("2024", "2023"),
                            List.of(new FinancialStatements.StatementRow("Revenue", revenues()))),
                    new FinancialStatements.StatementTable("Balance Sheet", List.of("2024"),
                            List.of(new FinancialStatements.StatementRow("Total assets", Map.of("2024", "359,241,000")))),
                    new FinancialStatements.StatementTable("Cash Flow Statement", List.of("2024"),
                            List.of(new FinancialStatements.StatementRow("Free cash flow", Map.of("2024", "0"))))
            )
    );

    private static Map<String, String> revenues() {
        Map<String, String> revenues = new LinkedHashMap<>();
        revenues.put("2025", "466,823,000");
        revenues.put("2024", null);
        return revenues;
    }


}