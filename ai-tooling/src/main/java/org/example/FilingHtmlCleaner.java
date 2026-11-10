package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public final class FilingHtmlCleaner {
    public Document clean(String rawHtml) {
        Document document = Jsoup.parse(rawHtml);

        document.select("script, style, meta, link, noscript").remove();
        document.select("ix|header, ix|hidden, ix|references, ix|resources").remove();
        document.select("[style]").forEach(element -> {
            String style = normalizeStyle(element.attr("style"));
            if (style.contains("display:none") || style.contains("visibility:hidden")) {
                element.remove();
            }
        });

        return document;
    }

    public static String normalizedText(Element element) {
        return normalizeText(element.text());
    }

    public static String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u00a0', ' ')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u2019', '\'')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String normalizeStyle(String style) {
        return style.toLowerCase()
                .replaceAll("\\s+", "")
                .trim();
    }
}
