package org.example.statements;

import org.example.statements.balancesheet.BalanceSheetMetricEnumType;

import java.util.Map;

public record StatementRow(BalanceSheetMetricEnumType metricType, Map<String, String> values) {
}
