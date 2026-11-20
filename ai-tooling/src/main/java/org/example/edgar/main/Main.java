package org.example.edgar.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.loader.dividendwatch.DividendWatchBrowserLogin;
import org.example.loader.dividendwatch.DividendWatchDataLoader;
import org.example.loader.edgar.EdgarDataLoader;
import org.example.statements.FinancialStatementRequest;
import org.example.statements.balancesheet.EdgarBalanceSheetService;
import org.example.statements.cashflow.EdgarCashFlowStatementService;
import org.example.statements.income.EdgarIncomeStatementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    static void main(String[] args) throws Exception {
        String ticker = args.length > 0 ? args[0].trim() : property("stock.ticker", "AAPL");
        String mode = args.length > 0 ? args[1].trim() : property("mode", "DIVIDEND_WATCH");

        if ("DIVIDEND_WATCH".equals(mode)) {
            var login = DividendWatchBrowserLogin.fromSystemProperties();
            var loader = new DividendWatchDataLoader(login);
            // Preserve the CLI's existing behavior of returning all scraped periods.
            var statements = loader.load(new FinancialStatementRequest(ticker, Integer.MAX_VALUE));
            LOGGER.info(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(statements));

        } else if ("EDGAR".equals(mode)) {
            String userAgent = property("edgar.user.agent", "ai-experiments test@example.com");
            var loader = new EdgarDataLoader(new EdgarBalanceSheetService(userAgent),
                    new EdgarIncomeStatementService(userAgent),
                    new EdgarCashFlowStatementService(userAgent));
            var statements = loader.load(new FinancialStatementRequest(ticker, 4));
            LOGGER.info(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(statements));
        } else {
            LOGGER.error("Unknown or unsupported mode: [{}]", mode);
        }


    }

    private static String property(String name, String fallback) {
        String value = System.getProperty(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
