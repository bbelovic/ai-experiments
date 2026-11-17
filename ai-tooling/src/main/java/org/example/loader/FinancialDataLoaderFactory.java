package org.example.loader;

@FunctionalInterface
public interface FinancialDataLoaderFactory {
    FinancialDataLoader create(String ticker, int annualPeriods);
}
