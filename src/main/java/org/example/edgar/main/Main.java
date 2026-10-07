package org.example.edgar.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.loader.dividendwatch.DividendWatchBrowserLogin;
import org.example.loader.dividendwatch.DividendWatchDataLoader;
import org.example.loader.edgar.EdgarDataLoader;
import org.example.statements.balancesheet.EdgarBalanceSheetService;
import org.example.statements.income.EdgarIncomeStatementService;

public class Main {
    static void main(String[] args) throws Exception {
        String ticker = args.length > 0 ? args[0].trim() : property("stock.ticker", "AAPL");
        String mode = args.length > 0 ? args[1].trim() : property("mode", "DIVIDEND_WATCH");

        if ("DIVIDEND_WATCH".equals(mode)) {
            var login = DividendWatchBrowserLogin.fromSystemProperties();
            var loader = new DividendWatchDataLoader(ticker, login);
            var statements = loader.load();
            System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(statements));


        } else if ("EDGAR".equals(mode)) {
            String userAgent = property("edgar.user.agent", "ai-experiments test@example.com");
            var loader = new EdgarDataLoader(ticker, new EdgarBalanceSheetService(userAgent, 4),
                    new EdgarIncomeStatementService(userAgent, 4));
            var statements = loader.load();
            System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(statements));
        }


    }

    private static String property(String name, String fallback) {
        String value = System.getProperty(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
