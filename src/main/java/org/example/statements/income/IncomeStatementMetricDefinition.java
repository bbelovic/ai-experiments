package org.example.statements.income;

import java.util.List;

record IncomeStatementMetricDefinition(
        String key,
        String label,
        String section,
        String unit,
        List<String> usGaapConcepts
) {
}
