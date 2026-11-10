package org.example;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FilingParserTest {
    private final FilingParser parser = new FilingParser();
    private final FilingHtmlCleaner cleaner = new FilingHtmlCleaner();

    @Test
    void cleansBasicSecHtmlAndRemovesNonContentAndHiddenElements() {
        Document document = cleaner.clean("""
                <html>
                <head><meta name="x"><link href="x"><style>.x{}</style><script>bad()</script></head>
                <body>
                    <p>Visible&nbsp;text &amp; entities</p>
                    <p style="visibility: hidden">invisible</p>
                    <noscript>noscript text</noscript>
                </body>
                </html>
                """);

        String text = FilingHtmlCleaner.normalizedText(document.body());

        assertThat(text).isEqualTo("Visible text & entities");
        assertThat(document.select("script, style, meta, link, noscript")).isEmpty();
    }

    @Test
    void extractsSectionsFromSimplified10KAndAvoidsTableOfContents() {
        FilingDocument document = parser.parseFiling(metadata(), "10-K", fixture("simple-10k.html"));

        assertThat(document.sections())
                .extracting(FilingSection::itemCode)
                .containsExactly("1", "1A", "7");
        assertThat(document.sections().getFirst().text())
                .contains("Apple designs")
                .doesNotContain("Table of Contents")
                .doesNotContain("hidden business text");
    }

    @Test
    void extractsSectionsFromSimplified10Q() {
        FilingDocument document = parser.parseFiling(metadata(), "10-Q", fixture("simple-10q.html"));

        assertThat(document.sections())
                .extracting(FilingSection::itemCode)
                .containsExactly("PART I ITEM 1", "PART I ITEM 2", "PART II ITEM 1A");
        assertThat(document.sections().get(1).text()).contains("quarterly operating results");
    }

    @Test
    void extractsSimpleTablesAndAssociatesThemWithNearestSection() {
        FilingDocument document = parser.parseFiling(metadata(), "10-K", fixture("simple-10k.html"));

        FilingSection mda = document.sections().stream()
                .filter(section -> section.itemCode().equals("7"))
                .findFirst()
                .orElseThrow();

        assertThat(mda.tables()).hasSize(1);
        assertThat(mda.tables().getFirst().title()).isEqualTo("Revenue by Segment");
        assertThat(mda.tables().getFirst().columns()).containsExactly("Segment", "Revenue");
        assertThat(mda.tables().getFirst().rows()).containsExactly(
                List.of("Products", "100"),
                List.of("Services", "50")
        );
    }

    @Test
    void chunksLongSectionsAndCarriesMetadataAndSectionInformation() {
        String longParagraph = "Management discussion sentence. ".repeat(420);
        FilingDocument document = new FilingDocument(
                "0000320193",
                "AAPL",
                "Apple Inc.",
                "10-K",
                "2025-10-31",
                "2025-09-27",
                "000032019325000001",
                "https://www.sec.gov/Archives/test.htm",
                List.of(new FilingSection("7", "Management's Discussion and Analysis",
                        longParagraph + "\n\n" + longParagraph + "\n\n" + longParagraph,
                        List.of()))
        );

        List<FilingChunk> chunks = parser.chunkForLlm(document);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.cik()).isEqualTo("0000320193");
            assertThat(chunk.ticker()).isEqualTo("AAPL");
            assertThat(chunk.sectionCode()).isEqualTo("7");
            assertThat(chunk.sectionTitle()).isEqualTo("Management's Discussion and Analysis");
        });
        assertThat(chunks).extracting(FilingChunk::chunkIndex).containsExactly(0, 1, 2);
    }

    @Test
    void gracefullyHandlesMissingSections() {
        FilingDocument document = parser.parseFiling(metadata(), "10-K", """
                <html><body>
                <h2>Item 1. Business</h2>
                <p>Only one section is present.</p>
                </body></html>
                """);

        assertThat(document.sections()).hasSize(1);
        assertThat(document.sections().getFirst().itemCode()).isEqualTo("1");
    }

    private FormMetadata metadata() {
        return new FormMetadata(
                "0000320193",
                "Apple Inc.",
                "AAPL",
                "2025-10-31",
                "2025-09-27",
                "000032019325000001",
                "https://www.sec.gov/Archives/test.htm"
        );
    }

    private String fixture(String name) {
        try (var input = getClass().getResourceAsStream("/fixtures/" + name)) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read fixture " + name, e);
        }
    }
}
