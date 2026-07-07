package org.example;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public final class FilingSectionExtractor {
    private static final Logger LOGGER = Logger.getLogger(FilingSectionExtractor.class.getName());

    private static final List<SectionDefinition> TEN_K_SECTIONS = List.of(
            new SectionDefinition("1", "Business", Pattern.compile("\\bITEM\\s+1\\b(?!\\s*A)")),
            new SectionDefinition("1A", "Risk Factors", Pattern.compile("\\bITEM\\s+1\\s*A\\b")),
            new SectionDefinition("1B", "Unresolved Staff Comments", Pattern.compile("\\bITEM\\s+1\\s*B\\b")),
            new SectionDefinition("1C", "Cybersecurity", Pattern.compile("\\bITEM\\s+1\\s*C\\b")),
            new SectionDefinition("2", "Properties", Pattern.compile("\\bITEM\\s+2\\b")),
            new SectionDefinition("3", "Legal Proceedings", Pattern.compile("\\bITEM\\s+3\\b")),
            new SectionDefinition("7", "Management's Discussion and Analysis", Pattern.compile("\\bITEM\\s+7\\b(?!\\s*A)")),
            new SectionDefinition("7A", "Quantitative and Qualitative Disclosures About Market Risk", Pattern.compile("\\bITEM\\s+7\\s*A\\b")),
            new SectionDefinition("8", "Financial Statements and Supplementary Data", Pattern.compile("\\bITEM\\s+8\\b")),
            new SectionDefinition("9A", "Controls and Procedures", Pattern.compile("\\bITEM\\s+9\\s*A\\b"))
    );

    private static final List<SectionDefinition> TEN_Q_SECTIONS = List.of(
            new SectionDefinition("PART I ITEM 1", "Financial Statements", Pattern.compile("\\bPART\\s+I\\b.*\\bITEM\\s+1\\b|\\bITEM\\s+1\\b")),
            new SectionDefinition("PART I ITEM 2", "Management's Discussion and Analysis", Pattern.compile("\\bPART\\s+I\\b.*\\bITEM\\s+2\\b|\\bITEM\\s+2\\b")),
            new SectionDefinition("PART I ITEM 3", "Quantitative and Qualitative Disclosures About Market Risk", Pattern.compile("\\bPART\\s+I\\b.*\\bITEM\\s+3\\b|\\bITEM\\s+3\\b")),
            new SectionDefinition("PART I ITEM 4", "Controls and Procedures", Pattern.compile("\\bPART\\s+I\\b.*\\bITEM\\s+4\\b|\\bITEM\\s+4\\b")),
            new SectionDefinition("PART II ITEM 1", "Legal Proceedings", Pattern.compile("\\bPART\\s+II\\b.*\\bITEM\\s+1\\b|\\bITEM\\s+1\\b")),
            new SectionDefinition("PART II ITEM 1A", "Risk Factors", Pattern.compile("\\bPART\\s+II\\b.*\\bITEM\\s+1\\s*A\\b|\\bITEM\\s+1\\s*A\\b"))
    );

    private final FilingTableExtractor tableExtractor = new FilingTableExtractor();

    public List<FilingSection> extractSections(Document document, String form) {
        List<Element> orderedElements = document.body().select("*");
        Map<Element, Integer> elementIndexes = indexElements(orderedElements);
        List<HeadingCandidate> candidates = findHeadingCandidates(orderedElements, elementIndexes);
        int tocEndIndex = estimateTableOfContentsEnd(orderedElements);

        List<SectionDefinition> definitions = "10-Q".equalsIgnoreCase(form) ? TEN_Q_SECTIONS : TEN_K_SECTIONS;
        List<SectionMatch> matches = findOrderedMatches(definitions, candidates, tocEndIndex);

        List<FilingSection> sections = new ArrayList<>();
        for (int i = 0; i < matches.size(); i++) {
            SectionMatch current = matches.get(i);
            int start = current.elementIndex();
            int end = i + 1 < matches.size() ? matches.get(i + 1).elementIndex() : orderedElements.size();

            String text = extractTextBetween(orderedElements, start, end);
            List<FilingTable> tables = tableExtractor.extractTablesBetween(orderedElements, start, end);
            sections.add(new FilingSection(
                    current.definition().code(),
                    current.definition().title(),
                    text,
                    tables
            ));
        }

        return sections;
    }

    private List<SectionMatch> findOrderedMatches(
            List<SectionDefinition> definitions,
            List<HeadingCandidate> candidates,
            int tocEndIndex
    ) {
        List<SectionMatch> matches = new ArrayList<>();
        int previousIndex = tocEndIndex;

        for (SectionDefinition definition : definitions) {
            Optional<HeadingCandidate> match = Optional.empty();
            for (HeadingCandidate candidate : candidates) {
                if (candidate.elementIndex() > previousIndex && definition.matches(candidate.normalizedText())) {
                    match = Optional.of(candidate);
                    break;
                }
            }

            if (match.isPresent()) {
                HeadingCandidate candidate = match.get();
                matches.add(new SectionMatch(definition, candidate.element(), candidate.elementIndex()));
                previousIndex = candidate.elementIndex();
            } else {
                LOGGER.fine(() -> "Missing expected filing section: " + definition.code() + " " + definition.title());
            }
        }

        return matches;
    }

    private List<HeadingCandidate> findHeadingCandidates(List<Element> orderedElements, Map<Element, Integer> elementIndexes) {
        List<HeadingCandidate> candidates = new ArrayList<>();
        for (Element element : orderedElements) {
            if (hasBlockChild(element) || hasAncestorWithSameText(element)) {
                continue;
            }
            String text = FilingHtmlCleaner.normalizedText(element);
            if (looksLikeHeading(text)) {
                candidates.add(new HeadingCandidate(element, elementIndexes.get(element), normalizeHeading(text)));
            }
        }
        return candidates;
    }

    private String extractTextBetween(List<Element> orderedElements, int startExclusive, int endExclusive) {
        List<String> paragraphs = new ArrayList<>();
        for (int i = startExclusive + 1; i < endExclusive; i++) {
            Element element = orderedElements.get(i);
            if (!isTextBlock(element) || hasBlockChild(element) || hasAncestor(element, "table")) {
                continue;
            }
            String text = FilingHtmlCleaner.normalizedText(element);
            if (!text.isBlank() && !looksLikeHeading(text)) {
                paragraphs.add(text);
            }
        }
        return String.join("\n\n", paragraphs);
    }

    private int estimateTableOfContentsEnd(List<Element> orderedElements) {
        int tocStart = -1;
        for (int i = 0; i < orderedElements.size(); i++) {
            String text = normalizeHeading(FilingHtmlCleaner.normalizedText(orderedElements.get(i)));
            if (text.equals("TABLE OF CONTENTS") || text.equals("INDEX")) {
                tocStart = i;
                break;
            }
        }
        if (tocStart < 0) {
            return -1;
        }

        int itemCount = 0;
        int lastTocItem = tocStart;
        Set<String> seenTocHeadings = new HashSet<>();
        for (int i = tocStart + 1; i < Math.min(orderedElements.size(), tocStart + 160); i++) {
            String text = FilingHtmlCleaner.normalizedText(orderedElements.get(i));
            if (looksLikeHeading(text)) {
                String normalized = normalizeHeading(text);
                if (itemCount >= 2 && seenTocHeadings.contains(normalized)) {
                    return lastTocItem;
                }
                seenTocHeadings.add(normalized);
                itemCount++;
                lastTocItem = i;
            }
            if (itemCount >= 3 && i - lastTocItem > 20) {
                break;
            }
        }
        return lastTocItem;
    }

    private boolean looksLikeHeading(String text) {
        if (text.isBlank() || text.length() > 220) {
            return false;
        }
        String normalized = normalizeHeading(text);
        return normalized.matches(".*\\bITEM\\s+\\d+\\s*[A-Z]?\\b.*")
                || normalized.matches(".*\\bPART\\s+[IVX]+\\b.*");
    }

    private String normalizeHeading(String text) {
        return FilingHtmlCleaner.normalizeText(text)
                .toUpperCase(Locale.ROOT)
                .replaceAll("[\\.:;,_()\\[\\]]", " ")
                .replaceAll("\\s*-\\s*", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isTextBlock(Element element) {
        String tag = element.tagName().toLowerCase(Locale.ROOT);
        return tag.matches("p|div|span|font|blockquote|li|h[1-6]");
    }

    private boolean hasBlockChild(Element element) {
        return !element.select("> p, > div, > table, > ul, > ol, > h1, > h2, > h3, > h4, > h5, > h6").isEmpty();
    }

    private boolean hasAncestorWithSameText(Element element) {
        String text = FilingHtmlCleaner.normalizedText(element);
        Element parent = element.parent();
        while (parent != null && !"body".equalsIgnoreCase(parent.tagName())) {
            if (text.equals(FilingHtmlCleaner.normalizedText(parent))) {
                return true;
            }
            parent = parent.parent();
        }
        return false;
    }

    private boolean hasAncestor(Element element, String tagName) {
        Element parent = element.parent();
        while (parent != null) {
            if (tagName.equalsIgnoreCase(parent.tagName())) {
                return true;
            }
            parent = parent.parent();
        }
        return false;
    }

    private Map<Element, Integer> indexElements(List<Element> orderedElements) {
        Map<Element, Integer> indexes = new HashMap<>();
        for (int i = 0; i < orderedElements.size(); i++) {
            indexes.put(orderedElements.get(i), i);
        }
        return indexes;
    }

    private record SectionDefinition(String code, String title, Pattern headingPattern) {
        boolean matches(String normalizedHeading) {
            return headingPattern.matcher(normalizedHeading).find();
        }
    }

    private record HeadingCandidate(Element element, int elementIndex, String normalizedText) {
    }

    private record SectionMatch(SectionDefinition definition, Element element, int elementIndex) {
    }
}
