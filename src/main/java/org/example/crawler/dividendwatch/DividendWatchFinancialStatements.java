package org.example.crawler.dividendwatch;

import java.util.List;
import java.util.Map;

public record DividendWatchFinancialStatements(
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
