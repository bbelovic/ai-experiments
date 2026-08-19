package org.example.statements.balancesheet;

import java.util.List;

record BalanceSheetMetricDefinition(
        String key,
        String label,
        String section,
        List<String> usGaapConcepts
) {
}
