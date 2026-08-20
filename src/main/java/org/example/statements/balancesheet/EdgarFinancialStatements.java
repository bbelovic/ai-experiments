package org.example.statements.balancesheet;

import java.util.List;
import java.util.Map;

public record EdgarFinancialStatements(
        String ticker,
        String source,
        String sourceUrl,
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
