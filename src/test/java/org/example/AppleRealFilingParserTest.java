package org.example;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppleRealFilingParserTest {
    private final FilingParser parser = new FilingParser();

    @Test
    void parsesSectionsFromRealApple10QFixture() {
        FilingDocument document = parser.parseFiling(metadata(), "10-Q", fixture("APPL.htm"));

        assertThat(document.sections())
                .extracting(FilingSection::itemCode)
                .containsExactly(
                        "PART I ITEM 1",
                        "PART I ITEM 2",
                        "PART I ITEM 3",
                        "PART I ITEM 4",
                        "PART II ITEM 1",
                        "PART II ITEM 1A"
        );

        FilingSection financialStatements = section(document, "PART I ITEM 1");
        assertThat(financialStatements.text())
                .contains("CONDENSED CONSOLIDATED STATEMENTS OF OPERATIONS")
                .contains("CONDENSED CONSOLIDATED BALANCE SHEETS")
                .doesNotContain("PART I - FINANCIAL INFORMATION");
        assertThat(financialStatements.tables()).isNotEmpty();

        FilingSection managementDiscussion = section(document, "PART I ITEM 2");
        assertThat(managementDiscussion.text())
                .contains("The following discussion should be read in conjunction")
                .contains("Segment Operating Performance")
                .contains("Liquidity and Capital Resources");

        FilingSection marketRisk = section(document, "PART I ITEM 3");
        assertThat(marketRisk.text())
                .contains("There have been no material changes to the Company's market risk");

        FilingSection controls = section(document, "PART I ITEM 4");
        assertThat(controls.text())
                .contains("Disclosure Controls and Procedures")
                .contains("Internal Control over Financial Reporting");

        FilingSection legalProceedings = section(document, "PART II ITEM 1");
        assertThat(legalProceedings.text())
                .contains("Epic Games");

        FilingSection riskFactors = section(document, "PART II ITEM 1A");
        assertThat(riskFactors.text())
                .contains("artificial intelligence technologies")
                .contains("the 2025 Form 10-K");

        assertThat(document.sections()).allSatisfy(section -> assertNoAdjacentDuplicateParagraphs(section.text()));
    }

    @Test
    void chunksRealAppleFixtureWithMetadataAndSectionReferences() {
        FilingDocument document = parser.parseFiling(metadata(), "10-Q", fixture("APPL.htm"));

        List<FilingChunk> chunks = parser.chunkForLlm(document);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.cik()).isEqualTo("0000320193");
            assertThat(chunk.ticker()).isEqualTo("AAPL");
            assertThat(chunk.companyName()).isEqualTo("Apple Inc.");
            assertThat(chunk.form()).isEqualTo("10-Q");
            assertThat(chunk.sectionCode()).isNotBlank();
            assertThat(chunk.sectionTitle()).isNotBlank();
            assertThat(chunk.sourceUrl()).contains("sec.gov/Archives");
        });
        assertThat(chunks).extracting(FilingChunk::chunkIndex)
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, chunks.size()).boxed().toList());
    }

    private FilingSection section(FilingDocument document, String code) {
        return document.sections().stream()
                .filter(section -> section.itemCode().equals(code))
                .findFirst()
                .orElseThrow();
    }

    private void assertNoAdjacentDuplicateParagraphs(String text) {
        String previous = "";
        for (String paragraph : text.split("\\R\\s*\\R")) {
            String current = FilingHtmlCleaner.normalizeText(paragraph);
            if (!current.isBlank()) {
                assertThat(current).isNotEqualTo(previous);
                previous = current;
            }
        }
    }

    private FormMetadata metadata() {
        return new FormMetadata(
                "0000320193",
                "Apple Inc.",
                "AAPL",
                "2026-05-01",
                "2026-03-28",
                "000032019326000000",
                "https://www.sec.gov/Archives/edgar/data/320193/test/aapl-20260328.htm"
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
