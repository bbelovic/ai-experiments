package org.example;

import java.util.List;

public record FilingDocument(
        String cik,
        String ticker,
        String companyName,
        String form,
        String filingDate,
        String reportDate,
        String accessionNumber,
        String sourceUrl,
        List<FilingSection> sections
) {
}
