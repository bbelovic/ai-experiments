package org.example;

public record FilingChunk(
        String cik,
        String ticker,
        String companyName,
        String form,
        String filingDate,
        String reportDate,
        String accessionNumber,
        String sourceUrl,
        String sectionCode,
        String sectionTitle,
        int chunkIndex,
        String text
) {
}
