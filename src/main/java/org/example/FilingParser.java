package org.example;

import org.jsoup.nodes.Document;

import java.util.List;

public final class FilingParser {
    private final FilingHtmlCleaner cleaner = new FilingHtmlCleaner();
    private final FilingSectionExtractor sectionExtractor = new FilingSectionExtractor();
    private final FilingChunker chunker = new FilingChunker();

    public FilingDocument parseFiling(FormMetadata metadata, String form, String rawHtml) {
        Document cleaned = cleaner.clean(rawHtml);
        List<FilingSection> sections = sectionExtractor.extractSections(cleaned, form);

        return new FilingDocument(
                metadata.cik(),
                metadata.ticker(),
                metadata.companyName(),
                form,
                metadata.filingDate(),
                metadata.reportDate(),
                metadata.accessionDate(),
                metadata.filingUrl(),
                sections
        );
    }

    public List<FilingChunk> chunkForLlm(FilingDocument filingDocument) {
        return chunker.chunkForLlm(filingDocument);
    }
}
