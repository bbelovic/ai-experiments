package org.example;

import java.util.List;

public record FilingSection(
        String itemCode,
        String title,
        String text,
        List<FilingTable> tables
) {
}
