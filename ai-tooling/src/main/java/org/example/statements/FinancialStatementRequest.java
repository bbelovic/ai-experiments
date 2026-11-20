package org.example.statements;

import java.util.Locale;

/** Request-specific inputs; annualPeriods is a maximum, not a calendar year. */
public record FinancialStatementRequest(String ticker, int annualPeriods) {
    public FinancialStatementRequest {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Invalid stock ticker: [%s]".formatted(ticker));
        }
        if (annualPeriods < 1) {
            throw new IllegalArgumentException("Invalid annualPeriods value: [%d]".formatted(annualPeriods));
        }
        ticker = ticker.trim().toUpperCase(Locale.ROOT);
    }
}
