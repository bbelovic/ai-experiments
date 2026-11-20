package org.example.loader.dividendwatch;

import org.example.loader.FinancialDataLoader;
import org.example.statements.FinancialStatements;
import org.example.statements.FinancialStatementRequest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DividendWatchDataLoader implements FinancialDataLoader {
    private final DividendWatchBrowserLogin browserLogin;

    public DividendWatchDataLoader(DividendWatchBrowserLogin browserLogin) {
        this.browserLogin = browserLogin;
    }

    @Override
    public FinancialStatements load(FinancialStatementRequest request) {
        FinancialStatements statements = browserLogin.scrapeFinancialStatements(request.ticker());
        return new FinancialStatements(statements.ticker(), statements.source(), statements.sourceUrl(),
                statements.statements().stream()
                        .map(table -> limitPeriods(table, request.annualPeriods()))
                        .toList());
    }

    private FinancialStatements.StatementTable limitPeriods(FinancialStatements.StatementTable table, int maximum) {
        if (table.periods().size() <= maximum) {
            return table;
        }
        var periods = table.periods().stream().limit(maximum).toList();
        var rows = table.rows().stream().map(row -> {
            Map<String, String> values = new LinkedHashMap<>();
            periods.forEach(period -> values.put(period, row.values().get(period)));
            return new FinancialStatements.StatementRow(row.metric(), Collections.unmodifiableMap(values));
        }).toList();
        return new FinancialStatements.StatementTable(table.name(), periods, rows);
    }
}
