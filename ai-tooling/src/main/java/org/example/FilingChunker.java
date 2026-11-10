package org.example;

import java.util.ArrayList;
import java.util.List;

public final class FilingChunker {
    private static final int TARGET_MIN_CHARS = 1_000 * 4;
    private static final int TARGET_MAX_CHARS = 2_500 * 4;
    private static final int OVERLAP_CHARS = 150 * 4;

    public List<FilingChunk> chunkForLlm(FilingDocument filingDocument) {
        List<FilingChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;

        for (FilingSection section : filingDocument.sections()) {
            List<String> sectionChunks = splitSection(section.text());
            for (String text : sectionChunks) {
                chunks.add(new FilingChunk(
                        filingDocument.cik(),
                        filingDocument.ticker(),
                        filingDocument.companyName(),
                        filingDocument.form(),
                        filingDocument.filingDate(),
                        filingDocument.reportDate(),
                        filingDocument.accessionNumber(),
                        filingDocument.sourceUrl(),
                        section.itemCode(),
                        section.title(),
                        chunkIndex++,
                        text
                ));
            }
        }

        return chunks;
    }

    private List<String> splitSection(String text) {
        String normalized = FilingHtmlCleaner.normalizeText(text).replace("\n \n", "\n\n");
        if (normalized.isBlank()) {
            return List.of();
        }
        if (normalized.length() <= TARGET_MAX_CHARS) {
            return List.of(normalized);
        }

        String[] paragraphs = text.split("\\R\\s*\\R");
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            String cleanParagraph = FilingHtmlCleaner.normalizeText(paragraph);
            if (cleanParagraph.isBlank()) {
                continue;
            }
            if (current.length() > 0 && current.length() + cleanParagraph.length() + 2 > TARGET_MAX_CHARS) {
                chunks.add(current.toString());
                current = new StringBuilder(overlapFrom(chunks.getLast()));
            }
            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(cleanParagraph);
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    private String overlapFrom(String text) {
        if (text.length() <= TARGET_MIN_CHARS) {
            return "";
        }
        int start = Math.max(0, text.length() - OVERLAP_CHARS);
        int paragraphStart = text.indexOf("\n\n", start);
        if (paragraphStart >= 0 && paragraphStart + 2 < text.length()) {
            return text.substring(paragraphStart + 2);
        }
        return text.substring(start);
    }
}
