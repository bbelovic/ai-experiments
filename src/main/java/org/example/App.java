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
        root.path("fillings").path("recent");
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
