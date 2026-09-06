package org.example.statements;

import java.util.Map;

public record StatementRow(String metric, Map<String, String> values) {
}
