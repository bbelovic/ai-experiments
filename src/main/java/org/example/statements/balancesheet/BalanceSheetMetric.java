package org.example.statements.balancesheet;

import java.math.BigDecimal;

public record BalanceSheetMetric(
        String key,
        String label,
        String section,
        String usGaapConcept,
        BigDecimal value,
        String unit,
        String reportDate,
        String filingDate,
        String accessionNumber
) {
}
