package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
        var recent = root.path("filings").path("recent");

        JsonNode forms = recent.path("form");
        JsonNode filingDates = recent.path("filingDate");
        JsonNode accessionNumbers = recent.path("accessionNumber");
        JsonNode primaryDocuments = recent.path("primaryDocument");

        for (var i = 0; i < forms.size(); i++) {
            String formText = forms.get(i).asText();
            if ("10-K".equals(formText) || "10-Q".equals(formText)) {
                String accessionDateNoDash = accessionNumbers.get(i).asText()
                        .replace("-", "");
                String primaryDocument = primaryDocuments.get(i).asText();
                String filingDate = filingDates.get(i).asText();

                String filingUrl = "https://www.sec.gov/Archives/edgar/data/"
                        + Long.parseLong(cik) + "/"
                        + accessionDateNoDash + "/"
                        + primaryDocument;

                System.out.println(formText + " | " + filingDate + " | " + filingUrl);
            }
        }

    }

    private static HttpRequest get(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .GET()
                .header("User-Agent", "EDGAR client")
                .header("Accept", "application/json")
                .build();
    }

    public static String greeting() {
        return "Hello, Java 25";
    }
}
