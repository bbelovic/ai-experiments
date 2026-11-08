package org.example.tools;

public class EdgarDataLoaderTool {

    void getFinancialStatements(String ticker, int years) {
        if (ticker == null || ticker.isBlank())
            throw new IllegalArgumentException("Invalid stock ticker: [%s]".formatted(ticker));
        if (years <= 0)
            throw new IllegalArgumentException("Invalid years value: [%d]".formatted(years));
    }
}
