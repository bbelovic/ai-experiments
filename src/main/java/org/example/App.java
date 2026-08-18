package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public final class App {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private App() {
    }

    static void main(String[] args) throws Exception {
        String ticker = args.length > 0 ? args[0] : "AAPL";
        List<FilingSection> sections = latest10QSections(ticker);

        for (var section : sections) {
            System.out.printf("""
                    %s
                    ================
                    %n""", section.title());
        }

    }

    private static List<FilingSection> latest10QSections(String ticker) throws Exception {
        FormMetadata metadata = latest10QMetadata(ticker);
        System.out.println("Getting: " + metadata.filingUrl());
        HttpResponse<String> rawResponse = CLIENT.send(getHtml(metadata.filingUrl()), HttpResponse.BodyHandlers.ofString());
        FilingDocument filingDocument = new FilingParser().parseFiling(metadata, "10-Q", rawResponse.body());

        return filingDocument.sections();
    }

    private static FormMetadata latest10QMetadata(String ticker) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode companies = fetchCompanyTickers(objectMapper);

        String cik = findCikForTicker(companies, ticker);
        String paddedCik = String.format("%010d", Long.parseLong(cik));
        JsonNode root = fetchSubmissions(objectMapper, paddedCik);
        JsonNode recent = root.path("filings").path("recent");

        JsonNode forms = recent.path("form");
        JsonNode filingDates = recent.path("filingDate");
        JsonNode reportDates = recent.path("reportDate");
        JsonNode accessionNumbers = recent.path("accessionNumber");
        JsonNode primaryDocuments = recent.path("primaryDocument");
        for (int i = 0; i < forms.size(); i++) {
            if ("10-Q".equals(forms.get(i).asText())) {
                String accessionNoDash = accessionNumbers.get(i).asText().replace("-", "");
                String filingUrl = "https://www.sec.gov/Archives/edgar/data/"
                        + Long.parseLong(cik) + "/"
                        + accessionNoDash + "/"
                        + primaryDocuments.get(i).asText();
                return new FormMetadata(
                        cik,
                        root.path("name").asText(),
                        getTicker(root),
                        filingDates.get(i).asText(),
                        reportDates.get(i).asText(),
                        accessionNoDash,
                        filingUrl
                );
            }
        }

        throw new IllegalArgumentException("No recent 10-Q found for ticker " + ticker);
    }

    private static JsonNode fetchCompanyTickers(ObjectMapper objectMapper) throws Exception {
        return objectMapper.readTree(CLIENT.send(
                get("https://www.sec.gov/files/company_tickers.json"),
                HttpResponse.BodyHandlers.ofString()
        ).body());
    }

    private static JsonNode fetchSubmissions(ObjectMapper objectMapper, String paddedCik) throws Exception {
        return objectMapper.readTree(CLIENT.send(
                get("https://data.sec.gov/submissions/CIK" + paddedCik + ".json"),
                HttpResponse.BodyHandlers.ofString()
        ).body());
    }

    private static String findCikForTicker(JsonNode companies, String ticker) {
        String normalizedTicker = ticker.trim().toUpperCase();
        for (JsonNode company : companies) {
            if (normalizedTicker.equals(company.path("ticker").asText().toUpperCase())) {
                return company.path("cik_str").asText();
            }
        }
        throw new IllegalArgumentException("Unknown ticker " + ticker);
    }

    private static HttpRequest get(String url) {
        return secRequest(url)
                .header("Accept", "application/json")
                .build();
    }

    private static HttpRequest getHtml(String url) {
        return secRequest(url)
                .header("Accept", "text/html,application/xhtml+xml")
                .build();
    }

    private static HttpRequest.Builder secRequest(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .GET()
                .header("User-Agent", "test@company.com");
    }

    public static String getTicker(JsonNode root) {
        return root.path("tickers").get(0).asText();
    }
}
