package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public final class App {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private App() {
    }

    static void main() throws Exception {
        String cik = "320193"; // Apple
        String paddedCik = String.format("%010d", Long.parseLong(cik));

        String submissionsUrl =
                "https://data.sec.gov/submissions/CIK" + paddedCik + ".json";
        var httpRequest = get(submissionsUrl);
        HttpResponse<String> response = CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(response.body());
        System.out.println(response.body());
        var recent = root.path("filings").path("recent");

        JsonNode companyName = root.path("name");
        String ticker = getTicker(root);
        JsonNode forms = recent.path("form");
        JsonNode filingDates = recent.path("filingDate");
        JsonNode reportDates = recent.path("reportDate");
        JsonNode accessionNumbers = recent.path("accessionNumber");
        JsonNode primaryDocuments = recent.path("primaryDocument");

        var metadataList = new ArrayList<FormMetadata>();
        for (var i = 0; i < forms.size(); i++) {
            String formText = forms.get(i).asText();
            if ("10-K".equals(formText) || "10-Q".equals(formText)) {
                String accessionDateNoDash = accessionNumbers.get(i).asText()
                        .replace("-", "");
                String primaryDocument = primaryDocuments.get(i).asText();
                String filingDate = filingDates.get(i).asText();
                String reportDate = reportDates.get(i).asText();

                String filingUrl = "https://www.sec.gov/Archives/edgar/data/"
                        + Long.parseLong(cik) + "/"
                        + accessionDateNoDash + "/"
                        + primaryDocument;

//                System.out.println(cik + " | " + companyName.asText()  + " | " + ticker + " | " +
//                        formText + " | " + filingDate + " | " + reportDate + " | " + filingUrl);
                metadataList.add(new FormMetadata(cik, companyName.asText(), ticker, filingDate, reportDate, accessionDateNoDash, filingUrl));
            }
        }

        FormMetadata formMetadata = metadataList.getFirst();
        FilingParser filingParser = new FilingParser();

        HttpResponse<String> rawResponse = CLIENT.send(getHtml(formMetadata.filingUrl()), HttpResponse.BodyHandlers.ofString());

//        System.out.println(rawResponse.body());


//        try (var pw = new PrintWriter("APPL.htm")) {
//
//            pw.write(rawResponse.body());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }



        FilingDocument filingDocument = filingParser.parseFiling(formMetadata, "10-K", rawResponse.body());
        List<FilingSection> sections = filingDocument.sections();
        for (var section : sections) {
            System.out.printf("""
                    %s
                    =========
                    %s
                    %n""", section.title(), section.text());
        }


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
