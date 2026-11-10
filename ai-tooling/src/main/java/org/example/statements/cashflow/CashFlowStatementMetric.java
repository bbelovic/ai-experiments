package org.example.statements.cashflow;

import java.math.BigDecimal;

record CashFlowStatementMetric(
        String key,
        String label,
        String section,
        String usGaapConcept,
        BigDecimal value,
        String unit,
        String startDate,
        String reportDate,
        String filingDate,
        String accessionNumber
) {
}
