package org.example.statements.cashflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.statements.FinancialStatements;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class EdgarCashFlowStatementService {
    private static final DecimalFormat VALUE_FORMAT = new DecimalFormat(
            "#,##0.###",
            DecimalFormatSymbols.getInstance(Locale.US)
    );

    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final String userAgent;
    private final int cashFlowStatementPeriods;

    public EdgarCashFlowStatementService(String userAgent, int cashFlowStatementPeriods) {
        this(HttpClient.newHttpClient(), new ObjectMapper(), userAgent, cashFlowStatementPeriods);
    }

    EdgarCashFlowStatementService(
            HttpClient client,
            ObjectMapper objectMapper,
            String userAgent,
            int cashFlowStatementPeriods
    ) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.userAgent = userAgent;
        this.cashFlowStatementPeriods = cashFlowStatementPeriods;
    }

    public FinancialStatements annualCashFlowStatement(String ticker) {
        JsonNode companies = fetchJson("https://www.sec.gov/files/company_tickers.json");
        String cik = findCikForTicker(companies, ticker);
        String paddedCik = String.format("%010d", Long.parseLong(cik));
        JsonNode submissions = fetchJson("https://data.sec.gov/submissions/CIK" + paddedCik + ".json");
        JsonNode companyFacts = fetchJson("https://data.sec.gov/api/xbrl/companyfacts/CIK" + paddedCik + ".json");

        return extractAnnualCashFlowStatement(ticker, submissions, companyFacts);
    }

    FinancialStatements extractAnnualCashFlowStatement(String ticker, JsonNode submissions, JsonNode companyFacts) {
        List<AnnualFiling> filings = latest10Ks(submissions, cashFlowStatementPeriods);
        List<String> periods = filings.stream()
                .map(AnnualFiling::fiscalYear)
                .toList();
        List<Map<String, CashFlowStatementMetric>> metricMaps = filings.stream()
                .map(filing -> metricsByKey(metricsForFiling(companyFacts, filing)))
                .toList();

        List<FinancialStatements.StatementRow> rows = new ArrayList<>();
        for (CashFlowStatementMetricEnumType definition : CashFlowStatementMetricEnumType.values()) {
            Map<String, String> values = new LinkedHashMap<>();
            for (int periodIndex = 0; periodIndex < periods.size(); periodIndex++) {
                CashFlowStatementMetric metric = metricMaps.get(periodIndex).get(definition.getKey());
                values.put(periods.get(periodIndex), metric == null ? null : formatValue(metric.value()));
            }
            rows.add(new FinancialStatements.StatementRow(
                    definition.getLabel(),
                    Collections.unmodifiableMap(new LinkedHashMap<>(values))
            ));
        }

        List<String> sourceUrl = filings.stream()
                .map(filing -> filing.sourceUrl(companyFacts.path("cik").asText()))
                .toList();
        return new FinancialStatements(
                ticker.trim().toUpperCase(Locale.ROOT),
                "EDGAR",
                sourceUrl,
                List.of(new FinancialStatements.StatementTable(
                        "Cash Flow Statement",
                        periods,
                        List.copyOf(rows)
                ))
        );
    }

    public static DecimalFormat getValueFormat() {
        return VALUE_FORMAT;
    }

    private List<CashFlowStatementMetric> metricsForFiling(JsonNode companyFacts, AnnualFiling filing) {
        List<CashFlowStatementMetric> metrics = new ArrayList<>();
        Map<String, CashFlowStatementMetric> metricsByKey = new LinkedHashMap<>();

        for (CashFlowStatementMetricEnumType definition : CashFlowStatementMetricEnumType.values()) {
            Optional<CashFlowStatementMetric> metric = findMetric(definition, companyFacts, filing)
                    .or(() -> deriveMetric(definition, metricsByKey, filing));
            metric.ifPresent(found -> {
                metrics.add(found);
                metricsByKey.put(found.key(), found);
            });
        }
        return List.copyOf(metrics);
    }

    private Map<String, CashFlowStatementMetric> metricsByKey(List<CashFlowStatementMetric> metrics) {
        Map<String, CashFlowStatementMetric> result = new LinkedHashMap<>();
        for (CashFlowStatementMetric metric : metrics) {
            result.put(metric.key(), metric);
        }
        return result;
    }

    private Optional<CashFlowStatementMetric> findMetric(
            CashFlowStatementMetricEnumType definition,
            JsonNode companyFacts,
            AnnualFiling filing
    ) {
        for (String concept : definition.getUsGAAPConcepts()) {
            Optional<JsonNode> fact = matchingFact(companyFacts, concept, definition.getUnit(), filing);
            if (fact.isPresent()) {
                JsonNode node = fact.get();
                return Optional.of(new CashFlowStatementMetric(
                        definition.getKey(),
                        definition.getLabel(),
                        definition.getSection(),
                        concept,
                        node.path("val").decimalValue(),
                        definition.getUnit(),
                        node.path("start").asText(),
                        node.path("end").asText(filing.reportDate()),
                        node.path("filed").asText(filing.filingDate()),
                        node.path("accn").asText(filing.accessionNumber())
                ));
            }
        }
        return Optional.empty();
    }

    private Optional<CashFlowStatementMetric> deriveMetric(
            CashFlowStatementMetricEnumType definition,
            Map<String, CashFlowStatementMetric> metricsByKey,
            AnnualFiling filing
    ) {
        return switch (definition) {
            case FREE_CASH_FLOW -> subtract(definition, filing, metricsByKey,
                    "operating_cash_flow", "capital_expenditures");
            default -> Optional.empty();
        };
    }

    private Optional<CashFlowStatementMetric> subtract(
            CashFlowStatementMetricEnumType definition,
            AnnualFiling filing,
            Map<String, CashFlowStatementMetric> metricsByKey,
            String minuendKey,
            String subtrahendKey
    ) {
        CashFlowStatementMetric minuend = metricsByKey.get(minuendKey);
        CashFlowStatementMetric subtrahend = metricsByKey.get(subtrahendKey);
        if (minuend == null || subtrahend == null
                || !minuend.startDate().equals(subtrahend.startDate())
                || !minuend.reportDate().equals(subtrahend.reportDate())) {
            return Optional.empty();
        }
        return Optional.of(derived(
                definition,
                filing,
                minuend.value().subtract(subtrahend.value()),
                minuendKey + "-" + subtrahendKey,
                minuend.startDate()
        ));
    }

    private CashFlowStatementMetric derived(
            CashFlowStatementMetricEnumType definition,
            AnnualFiling filing,
            BigDecimal value,
            String formula,
            String startDate
    ) {
        return new CashFlowStatementMetric(
                definition.getKey(),
                definition.getLabel(),
                definition.getSection(),
                "derived:" + formula,
                value,
                definition.getUnit(),
                startDate,
                filing.reportDate(),
                filing.filingDate(),
                filing.accessionNumber()
        );
    }

    private Optional<JsonNode> matchingFact(JsonNode companyFacts, String concept, String unit, AnnualFiling filing) {
        JsonNode facts = companyFacts.path("facts").path("us-gaap").path(concept).path("units").path(unit);
        if (!facts.isArray()) {
            return Optional.empty();
        }

        List<JsonNode> matches = new ArrayList<>();
        for (JsonNode fact : facts) {
            if (filing.accessionNumber().equals(fact.path("accn").asText())
                    && "FY".equals(fact.path("fp").asText())
                    && filing.reportDate().equals(fact.path("end").asText())
                    && fact.path("val").isNumber()
                    && durationDays(fact) >= 350
                    && durationDays(fact) <= 380) {
                matches.add(fact);
            }
        }

        return matches.stream()
                .max(Comparator
                        .comparingLong(this::durationDays)
                        .thenComparing(fact -> fact.path("filed").asText("")));
    }

    private long durationDays(JsonNode fact) {
        try {
            return ChronoUnit.DAYS.between(
                    LocalDate.parse(fact.path("start").asText()),
                    LocalDate.parse(fact.path("end").asText())) + 1;
        } catch (DateTimeParseException exception) {
            return -1;
        }
    }

    private List<AnnualFiling> latest10Ks(JsonNode submissions, int limit) {
        JsonNode recent = submissions.path("filings").path("recent");
        JsonNode forms = recent.path("form");
        JsonNode filingDates = recent.path("filingDate");
        JsonNode reportDates = recent.path("reportDate");
        JsonNode accessionNumbers = recent.path("accessionNumber");
        JsonNode fiscalYears = recent.path("fy");
        JsonNode primaryDocuments = recent.path("primaryDocument");

        Map<String, AnnualFiling> latestByFiscalYear = new LinkedHashMap<>();
        for (int filingIndex = 0; filingIndex < forms.size(); filingIndex++) {
            if (!"10-K".equals(forms.get(filingIndex).asText())) {
                continue;
            }
            String fiscalYear = fiscalYears.path(filingIndex).asText("");
            if (fiscalYear.isBlank()) {
                fiscalYear = reportDates.path(filingIndex).asText("").replaceFirst("^(\\d{4}).*", "$1");
            }
            AnnualFiling candidate = new AnnualFiling(
                    forms.get(filingIndex).asText(),
                    accessionNumbers.get(filingIndex).asText(),
                    filingDates.get(filingIndex).asText(),
                    reportDates.get(filingIndex).asText(),
                    fiscalYear,
                    primaryDocuments.path(filingIndex).asText("")
            );
            AnnualFiling existing = latestByFiscalYear.get(candidate.fiscalYear());
            if (existing == null || candidate.filingDate().compareTo(existing.filingDate()) > 0) {
                latestByFiscalYear.put(candidate.fiscalYear(), candidate);
            }
        }

        List<AnnualFiling> filings = latestByFiscalYear.values().stream()
                .sorted(Comparator.comparing(AnnualFiling::filingDate).reversed())
                .limit(limit)
                .toList();
        if (filings.isEmpty()) {
            throw new IllegalArgumentException("No recent 10-K found in submissions data");
        }
        return filings;
    }

    private String formatValue(BigDecimal value) {
        synchronized (VALUE_FORMAT) {
            return VALUE_FORMAT.format(value.stripTrailingZeros());
        }
    }

    private JsonNode fetchJson(String url) {
        try {
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(URI.create(url))
                            .GET()
                            .header("Accept", "application/json")
                            .header("User-Agent", userAgent)
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("SEC request failed with HTTP " + response.statusCode() + " for " + url);
            }
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("SEC request interrupted", exception);
        }
    }

    private String findCikForTicker(JsonNode companies, String ticker) {
        String normalizedTicker = ticker.trim().toUpperCase(Locale.ROOT);
        for (JsonNode company : companies) {
            if (normalizedTicker.equals(company.path("ticker").asText().toUpperCase(Locale.ROOT))) {
                return company.path("cik_str").asText();
            }
        }
        throw new IllegalArgumentException("Unknown ticker " + ticker);
    }

    private record AnnualFiling(
            String form,
            String accessionNumber,
            String filingDate,
            String reportDate,
            String fiscalYear,
            String primaryDocument
    ) {
        String sourceUrl(String cik) {
            if (primaryDocument == null || primaryDocument.isBlank()) {
                return "https://data.sec.gov/api/xbrl/companyfacts/CIK" + String.format("%010d", Long.parseLong(cik)) + ".json";
            }
            return "https://www.sec.gov/Archives/edgar/data/"
                    + Long.parseLong(cik)
                    + "/"
                    + accessionNumber.replace("-", "")
                    + "/"
                    + primaryDocument;
        }
    }
}
