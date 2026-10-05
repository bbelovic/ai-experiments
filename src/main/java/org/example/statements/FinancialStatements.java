package org.example.statements;

import java.util.List;
import java.util.Map;

public record FinancialStatements(
        String ticker,
        String source,
        List<String> sourceUrl,
        List<StatementTable> statements
) {
    public record StatementTable(
            String name,
            List<String> periods,
            List<StatementRow> rows
    ) {
    }

    public record StatementRow(
            String metric,
            Map<String, String> values
    ) {
    }
}
