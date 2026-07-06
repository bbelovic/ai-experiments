package org.example;

public record FormMetadata(String cik, String companyName, String ticker, String filingDate, String reportDate,
                           String accessionDate, String filingUrl) {
}
