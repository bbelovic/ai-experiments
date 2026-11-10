package org.example;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public final class FilingTableExtractor {
    public List<FilingTable> extractTablesBetween(List<Element> orderedElements, int startExclusive, int endExclusive) {
        List<FilingTable> tables = new ArrayList<>();
        for (int i = startExclusive + 1; i < endExclusive; i++) {
            Element element = orderedElements.get(i);
            if ("table".equalsIgnoreCase(element.tagName()) && !hasTableAncestor(element)) {
                FilingTable table = extractTable(element);
                if (!table.rows().isEmpty() || !table.columns().isEmpty()) {
                    tables.add(table);
                }
            }
        }
        return tables;
    }

    public FilingTable extractTable(Element table) {
        String title = findTitle(table);
        List<List<String>> rows = new ArrayList<>();

        for (Element row : table.select("> tbody > tr, > tr, > thead > tr")) {
            List<String> cells = row.select("> th, > td")
                    .stream()
                    .map(FilingHtmlCleaner::normalizedText)
                    .filter(cell -> !cell.isBlank())
                    .toList();
            if (!cells.isEmpty()) {
                rows.add(cells);
            }
        }

        List<String> columns = List.of();
        if (!rows.isEmpty() && table.select("th").size() > 0) {
            columns = rows.removeFirst();
        }

        return new FilingTable(title, columns, rows);
    }

    private String findTitle(Element table) {
        Elements captions = table.select("> caption");
        if (!captions.isEmpty()) {
            return FilingHtmlCleaner.normalizedText(captions.first());
        }

        Element previous = table.previousElementSibling();
        while (previous != null) {
            String text = FilingHtmlCleaner.normalizedText(previous);
            if (!text.isBlank()) {
                return text.length() > 140 ? "" : text;
            }
            previous = previous.previousElementSibling();
        }
        return "";
    }

    private boolean hasTableAncestor(Element element) {
        Element parent = element.parent();
        while (parent != null) {
            if ("table".equalsIgnoreCase(parent.tagName())) {
                return true;
            }
            parent = parent.parent();
        }
        return false;
    }
}
