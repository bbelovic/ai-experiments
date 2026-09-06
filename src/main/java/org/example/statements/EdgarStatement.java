package org.example.statements;

import java.util.List;

public record EdgarStatement(String ticker, String source, List<String> sourceUrl,
                             List<CommonStatementTable> statements) {
}
