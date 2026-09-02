package org.example.statements.income;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class EdgarIncomeStatementService {
    private static final DecimalFormat VALUE_FORMAT = new DecimalFormat(
            "#,##0.###",
            DecimalFormatSymbols.getInstance(Locale.US)
    );

    private static final List<IncomeStatementMetricDefinition> METRICS = List.of(
            metric("revenue", "Revenue", "Revenue", "USD",
                    "RevenueFromContractWithCustomerExcludingAssessedTax",
                    "Revenues",
                    "SalesRevenueNet"),
            metric("cost_of_revenue", "Cost of revenue", "Revenue", "USD",
                    "CostOfRevenue",
                    "CostOfGoodsAndServicesSold",
                    "CostOfGoodsSold"),
            metric("gross_profit", "Gross profit", "Revenue", "USD", "GrossProfit"),
            metric("research_and_development", "Research & development", "Operating expenses", "USD",
                    "ResearchAndDevelopmentExpense"),
            metric("selling_general_and_administrative", "Selling, general & administrative", "Operating expenses", "USD",
                    "SellingGeneralAndAdministrativeExpense",
                    "SellingAndMarketingExpense",
                    "GeneralAndAdministrativeExpense"),
            metric("operating_expenses", "Operating expenses", "Operating expenses", "USD", "OperatingExpenses"),
            metric("operating_income", "Operating income", "Income", "USD", "OperatingIncomeLoss"),
            metric("non_operating_income_expense", "Non-operating income/expense", "Income", "USD",
                    "NonoperatingIncomeExpense",
                    "OtherNonoperatingIncomeExpense"),
            metric("pretax_income", "Pretax income", "Income", "USD",
                    "IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest",
                    "IncomeLossFromContinuingOperationsBeforeIncomeTaxesMinorityInterestAndIncomeLossFromEquityMethodInvestments"),
            metric("income_tax_expense", "Income tax expense", "Income", "USD", "IncomeTaxExpenseBenefit"),
            metric("net_income", "Net income", "Income", "USD", "NetIncomeLoss"),
            metric("basic_eps", "Basic EPS", "Per share", "USD/shares", "EarningsPerShareBasic"),
            metric("diluted_eps", "Diluted EPS", "Per share", "USD/shares", "EarningsPerShareDiluted"),
            metric("basic_shares", "Basic shares", "Shares", "shares", "WeightedAverageNumberOfSharesOutstandingBasic"),
            metric("diluted_shares", "Diluted shares", "Shares", "shares", "WeightedAverageNumberOfDilutedSharesOutstanding")
    );

    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final String userAgent;
    private final int incomeStatementPeriods;

    public EdgarIncomeStatementService(String userAgent, int incomeStatementPeriods) {
        this(HttpClient.newHttpClient(), new ObjectMapper(), userAgent, incomeStatementPeriods);
    }

    EdgarIncomeStatementService(
            HttpClient client,
            ObjectMapper objectMapper,
            String userAgent,
            int incomeStatementPeriods
    ) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.userAgent = userAgent;
        this.incomeStatementPeriods = incomeStatementPeriods;
    }

    public EdgarIncomeStatement annualIncomeStatement(String ticker) throws IOException, InterruptedException {
        JsonNode companies = fetchJson("https://www.sec.gov/files/company_tickers.json");
        String cik = findCikForTicker(companies, ticker);
        String paddedCik = String.format("%010d", Long.parseLong(cik));
        JsonNode submissions = fetchJson("https://data.sec.gov/submissions/CIK" + paddedCik + ".json");
        JsonNode companyFacts = fetchJson("https://data.sec.gov/api/xbrl/companyfacts/CIK" + paddedCik + ".json");

        return extractAnnualIncomeStatement(ticker, submissions, companyFacts);
    }

    EdgarIncomeStatement extractAnnualIncomeStatement(String ticker, JsonNode submissions, JsonNode companyFacts) {
        List<AnnualFiling> filings = latest10Ks(submissions, incomeStatementPeriods);
        List<String> periods = filings.stream()
                .map(AnnualFiling::fiscalYear)
                .toList();
        List<Map<String, IncomeStatementMetric>> metricMaps = filings.stream()
                .map(filing -> metricsByKey(metricsForFiling(companyFacts, filing)))
                .toList();

        List<EdgarIncomeStatement.StatementRow> rows = new ArrayList<>();
        for (IncomeStatementMetricDefinition definition : METRICS) {
            Map<String, String> values = new LinkedHashMap<>();
            for (int i = 0; i < periods.size(); i++) {
                IncomeStatementMetric metric = metricMaps.get(i).get(definition.key());
                values.put(periods.get(i), metric == null ? "0" : formatValue(metric.value()));
            }
            rows.add(new EdgarIncomeStatement.StatementRow(
                    definition.label(),
                    Collections.unmodifiableMap(new LinkedHashMap<>(values))
            ));
        }

        List<String> sourceUrl = filings.stream()
                .map(filing -> filing.sourceUrl(companyFacts.path("cik").asText()))
                .toList();
        return new EdgarIncomeStatement(
                ticker.trim().toUpperCase(Locale.ROOT),
                "EDGAR",
                sourceUrl,
                List.of(new EdgarIncomeStatement.StatementTable(
                        "Income Statement",
                        periods,
                        List.copyOf(rows)
                ))
        );
    }

    public static DecimalFormat getValueFormat() {
        return VALUE_FORMAT;
    }

    private List<IncomeStatementMetric> metricsForFiling(JsonNode companyFacts, AnnualFiling filing) {
        List<IncomeStatementMetric> metrics = new ArrayList<>();
        Map<String, IncomeStatementMetric> metricsByKey = new LinkedHashMap<>();

        for (IncomeStatementMetricDefinition definition : METRICS) {
            Optional<IncomeStatementMetric> metric = findMetric(definition, companyFacts, filing)
                    .or(() -> deriveMetric(definition, metricsByKey, filing));
            metric.ifPresent(found -> {
                metrics.add(found);
                metricsByKey.put(found.key(), found);
            });
        }
        return List.copyOf(metrics);
    }

    private Map<String, IncomeStatementMetric> metricsByKey(List<IncomeStatementMetric> metrics) {
        Map<String, IncomeStatementMetric> result = new LinkedHashMap<>();
        for (IncomeStatementMetric metric : metrics) {
            result.put(metric.key(), metric);
        }
        return result;
    }

    private Optional<IncomeStatementMetric> findMetric(
            IncomeStatementMetricDefinition definition,
            JsonNode companyFacts,
            AnnualFiling filing
    ) {
        for (String concept : definition.usGaapConcepts()) {
            Optional<JsonNode> fact = matchingFact(companyFacts, concept, definition.unit(), filing);
            if (fact.isPresent()) {
                JsonNode node = fact.get();
                return Optional.of(new IncomeStatementMetric(
                        definition.key(),
                        definition.label(),
                        definition.section(),
                        concept,
                        node.path("val").decimalValue(),
                        definition.unit(),
                        node.path("end").asText(filing.reportDate()),
                        node.path("filed").asText(filing.filingDate()),
                        node.path("accn").asText(filing.accessionNumber())
                ));
            }
        }
        return Optional.empty();
    }

    private Optional<IncomeStatementMetric> deriveMetric(
            IncomeStatementMetricDefinition definition,
            Map<String, IncomeStatementMetric> metricsByKey,
            AnnualFiling filing
    ) {
        return switch (definition.key()) {
            case "gross_profit" -> subtract(definition, filing, metricsByKey, "revenue", "cost_of_revenue");
            case "operating_expenses" -> sum(definition, filing, metricsByKey,
                    "research_and_development",
                    "selling_general_and_administrative");
            case "operating_income" -> subtract(definition, filing, metricsByKey, "gross_profit", "operating_expenses");
            case "pretax_income" -> sum(definition, filing, metricsByKey,
                    "operating_income",
                    "non_operating_income_expense");
            default -> Optional.empty();
        };
    }

    private Optional<IncomeStatementMetric> sum(
            IncomeStatementMetricDefinition definition,
            AnnualFiling filing,
            Map<String, IncomeStatementMetric> metricsByKey,
            String firstKey,
            String secondKey
    ) {
        IncomeStatementMetric first = metricsByKey.get(firstKey);
        IncomeStatementMetric second = metricsByKey.get(secondKey);
        if (first == null || second == null) {
            return Optional.empty();
        }
        return Optional.of(derived(definition, filing, first.value().add(second.value()), firstKey + "+" + secondKey));
    }

    private Optional<IncomeStatementMetric> subtract(
            IncomeStatementMetricDefinition definition,
            AnnualFiling filing,
            Map<String, IncomeStatementMetric> metricsByKey,
            String minuendKey,
            String subtrahendKey
    ) {
        IncomeStatementMetric minuend = metricsByKey.get(minuendKey);
        IncomeStatementMetric subtrahend = metricsByKey.get(subtrahendKey);
        if (minuend == null || subtrahend == null) {
            return Optional.empty();
        }
        return Optional.of(derived(
                definition,
                filing,
                minuend.value().subtract(subtrahend.value()),
                minuendKey + "-" + subtrahendKey
        ));
    }

    private IncomeStatementMetric derived(
            IncomeStatementMetricDefinition definition,
            AnnualFiling filing,
            BigDecimal value,
            String formula
    ) {
        return new IncomeStatementMetric(
                definition.key(),
                definition.label(),
                definition.section(),
                "derived:" + formula,
                value,
                definition.unit(),
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
                    && fact.path("val").isNumber()) {
                matches.add(fact);
            }
        }

        return matches.stream()
                .max(Comparator
                        .comparing((JsonNode fact) -> fact.path("start").asText(""))
                        .thenComparing(fact -> fact.path("filed").asText("")));
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
        for (int i = 0; i < forms.size(); i++) {
            if (!"10-K".equals(forms.get(i).asText())) {
                continue;
            }
            String fiscalYear = fiscalYears.path(i).asText("");
            if (fiscalYear.isBlank()) {
                fiscalYear = reportDates.path(i).asText("").replaceFirst("^(\\d{4}).*", "$1");
            }
            AnnualFiling candidate = new AnnualFiling(
                    forms.get(i).asText(),
                    accessionNumbers.get(i).asText(),
                    filingDates.get(i).asText(),
                    reportDates.get(i).asText(),
                    fiscalYear,
                    primaryDocuments.path(i).asText("")
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

    private JsonNode fetchJson(String url) throws IOException, InterruptedException {
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

    private static IncomeStatementMetricDefinition metric(
            String key,
            String label,
            String section,
            String unit,
            String... concepts
    ) {
        return new IncomeStatementMetricDefinition(key, label, section, unit, List.of(concepts));
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
