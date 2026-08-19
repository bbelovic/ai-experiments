package org.example.statements.balancesheet;

import java.util.List;

public record BalanceSheetSnapshot(
        String cik,
        String ticker,
        String companyName,
        String form,
        String fiscalYear,
        String reportDate,
        String filingDate,
        String accessionNumber,
        List<BalanceSheetMetric> metrics
) {
}
