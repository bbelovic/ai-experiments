package org.example.statements.income;

import java.math.BigDecimal;

record IncomeStatementMetric(
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
