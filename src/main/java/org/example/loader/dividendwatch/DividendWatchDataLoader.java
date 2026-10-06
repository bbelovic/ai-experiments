package org.example.loader.dividendwatch;

import org.example.loader.FinancialDataLoader;
import org.example.statements.FinancialStatements;

public class DividendWatchDataLoader implements FinancialDataLoader {
    private final String ticker;
    private final DividendWatchBrowserLogin browserLogin;

    public DividendWatchDataLoader(String ticker, DividendWatchBrowserLogin browserLogin) {
        this.ticker = ticker;
        this.browserLogin = browserLogin;
    }

    @Override
    public FinancialStatements load() {
        return browserLogin.scrapeFinancialStatements(ticker);
    }
}
