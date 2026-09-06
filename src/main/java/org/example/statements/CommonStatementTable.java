package org.example.statements;

import java.util.List;

public record CommonStatementTable(String name, List<String> periods, List<StatementRow> rows) {
}
