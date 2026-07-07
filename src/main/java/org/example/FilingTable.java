package org.example;

import java.util.List;

public record FilingTable(
        String title,
        List<String> columns,
        List<List<String>> rows
) {
}
