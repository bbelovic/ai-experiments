package org.example.statements.balancesheet;

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

public final class EdgarBalanceSheetService {
    private static final int BALANCE_SHEET_PERIODS = 4;
    private static final DecimalFormat VALUE_FORMAT = new DecimalFormat(
            "#,##0.###",
            DecimalFormatSymbols.getInstance(Locale.US)
    );

    private static final List<BalanceSheetMetricDefinition> METRICS = List.of(
            metric("total_assets", "Total assets", "Assets", "Assets"),
            metric("current_assets", "Current assets", "Assets", "AssetsCurrent"),
            metric("cash_and_cash_equivalents", "Cash & cash equivalents", "Assets",
                    "CashAndCashEquivalentsAtCarryingValue",
                    "CashCashEquivalentsRestrictedCashAndRestrictedCashEquivalents"),
            metric("short_term_investments", "Short-term investments", "Assets",
                    "ShortTermInvestments",
                    "MarketableSecuritiesCurrent"),
            metric("cash_and_short_term_investments", "Cash & short-term investments", "Assets"),
            metric("receivables", "Receivables", "Assets",
                    "AccountsReceivableNetCurrent",
                    "AccountsReceivableNet",
                    "ReceivablesNetCurrent"),
            metric("inventory", "Inventory", "Assets", "InventoryNet"),
            metric("other_current_assets", "Other current assets", "Assets", "OtherCurrentAssets"),
            metric("non_current_assets", "Non-current assets", "Assets"),
            metric("ppe", "PP&E", "Assets",
                    "PropertyPlantAndEquipmentNet",
                    "PropertyPlantAndEquipmentAndFinanceLeaseRightOfUseAssetAfterAccumulatedDepreciationAndAmortization"),
            metric("goodwill", "Goodwill", "Assets", "Goodwill"),
            metric("intangible_assets", "Intangible assets", "Assets",
                    "FiniteLivedIntangibleAssetsNet",
                    "IntangibleAssetsNetExcludingGoodwill",
                    "IntangibleAssetsNetIncludingGoodwill"),
            metric("long_term_investments", "Long-term investments", "Assets",
                    "MarketableSecuritiesNoncurrent",
                    "LongTermInvestments"),
            metric("tax_assets", "Tax assets", "Assets",
                    "DeferredTaxAssetsNet",
                    "DeferredTaxAssetsNetCurrent",
                    "DeferredTaxAssetsLiabilitiesNet"),
            metric("other_non_current_assets", "Other non-current assets", "Assets", "OtherAssetsNoncurrent"),
            metric("total_liabilities", "Total liabilities", "Liabilities", "Liabilities"),
            metric("current_liabilities", "Current liabilities", "Liabilities", "LiabilitiesCurrent"),
            metric("accounts_payable", "Accounts payable", "Liabilities", "AccountsPayableCurrent"),
            metric("short_term_debt", "Short-term debt", "Liabilities",
                    "ShortTermBorrowings",
                    "ShortTermDebtCurrent",
                    "LongTermDebtCurrent"),
            metric("tax_payables", "Tax payables", "Liabilities", "TaxesPayableCurrent"),
            metric("current_deferred_revenue", "Deferred revenue", "Liabilities",
                    "ContractWithCustomerLiabilityCurrent",
                    "DeferredRevenueCurrent"),
            metric("other_current_liabilities", "Other current liabilities", "Liabilities", "OtherCurrentLiabilities"),
            metric("non_current_liabilities", "Non-current liabilities", "Liabilities", "LiabilitiesNoncurrent"),
            metric("long_term_debt", "Long-term debt", "Liabilities",
                    "LongTermDebtNoncurrent",
                    "LongTermDebtAndFinanceLeaseObligationsNoncurrent"),
            metric("non_current_deferred_revenue", "Deferred revenue", "Liabilities",
                    "ContractWithCustomerLiabilityNoncurrent",
                    "DeferredRevenueNoncurrent"),
            metric("deferred_tax", "Deferred tax", "Liabilities",
                    "DeferredTaxLiabilitiesNoncurrent",
                    "DeferredTaxLiabilitiesNet"),
            metric("other_non_current_liabilities", "Other non-current liabilities", "Liabilities", "OtherLiabilitiesNoncurrent"),
            metric("total_equity", "Total equity", "Equity",
                    "StockholdersEquity",
                    "StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest"),
            metric("preferred_stock", "Preferred stock", "Equity",
                    "PreferredStocksIncludingAdditionalPaidInCapital",
                    "PreferredStockValue"),
            metric("common_stock", "Common stock", "Equity",
                    "CommonStocksIncludingAdditionalPaidInCapital",
                    "CommonStockValue"),
            metric("retained_earnings", "Retained earnings", "Equity", "RetainedEarningsAccumulatedDeficit"),
            metric("aoci", "AOCI", "Equity", "AccumulatedOtherComprehensiveIncomeLossNetOfTax"),
            metric("other_equity", "Other equity", "Equity")
    );

    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final String userAgent;

    public EdgarBalanceSheetService(String userAgent) {
        this(HttpClient.newHttpClient(), new ObjectMapper(), userAgent);
    }

    EdgarBalanceSheetService(HttpClient client, ObjectMapper objectMapper, String userAgent) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.userAgent = userAgent;
    }

    public EdgarFinancialStatements annualBalanceSheetStatement(String ticker) throws IOException, InterruptedException {
        JsonNode companies = fetchJson("https://www.sec.gov/files/company_tickers.json");
        String cik = findCikForTicker(companies, ticker);
        String paddedCik = String.format("%010d", Long.parseLong(cik));
        JsonNode submissions = fetchJson("https://data.sec.gov/submissions/CIK" + paddedCik + ".json");
        JsonNode companyFacts = fetchJson("https://data.sec.gov/api/xbrl/companyfacts/CIK" + paddedCik + ".json");

        return extractAnnualBalanceSheetStatement(ticker, submissions, companyFacts);
    }

    EdgarFinancialStatements extractAnnualBalanceSheetStatement(
            String ticker,
            JsonNode submissions,
            JsonNode companyFacts
    ) {
        List<AnnualFiling> filings = latest10Ks(submissions, BALANCE_SHEET_PERIODS);
        List<String> periods = filings.stream()
                .map(AnnualFiling::fiscalYear)
                .toList();
        List<Map<String, BalanceSheetMetric>> metricMaps = filings.stream()
                .map(filing -> metricsByKey(metricsForFiling(companyFacts, filing)))
                .toList();

        List<EdgarFinancialStatements.StatementRow> rows = new ArrayList<>();
        for (BalanceSheetMetricDefinition definition : METRICS) {
            Map<String, String> values = new LinkedHashMap<>();
            for (int i = 0; i < periods.size(); i++) {
                BalanceSheetMetric metric = metricMaps.get(i).get(definition.key());
                values.put(periods.get(i), metric == null ? "0" : formatValue(metric.value()));
            }
            rows.add(new EdgarFinancialStatements.StatementRow(
                    definition.label(),
                    Collections.unmodifiableMap(new LinkedHashMap<>(values))
            ));
        }

        List<String> sourceUrl = filings.stream()
                .map(filing -> filing.sourceUrl(companyFacts.path("cik").asText()))
                .toList();
        return new EdgarFinancialStatements(
                ticker.trim().toUpperCase(Locale.ROOT),
                "EDGAR",
                sourceUrl,
                List.of(new EdgarFinancialStatements.StatementTable(
                        "Balance Sheet",
                        periods,
                        List.copyOf(rows)
                ))
        );
    }

    private List<BalanceSheetMetric> metricsForFiling(JsonNode companyFacts, AnnualFiling filing) {
        List<BalanceSheetMetric> metrics = new ArrayList<>();
        Map<String, BalanceSheetMetric> metricsByKey = new LinkedHashMap<>();

        for (BalanceSheetMetricDefinition definition : METRICS) {
            Optional<BalanceSheetMetric> metric = findMetric(definition, companyFacts, filing)
                    .or(() -> deriveMetric(definition, metricsByKey, filing));
            metric.ifPresent(found -> {
                metrics.add(found);
                metricsByKey.put(found.key(), found);
            });
        }
        return List.copyOf(metrics);
    }

    private Map<String, BalanceSheetMetric> metricsByKey(List<BalanceSheetMetric> metrics) {
        Map<String, BalanceSheetMetric> result = new LinkedHashMap<>();
        for (BalanceSheetMetric metric : metrics) {
            result.put(metric.key(), metric);
        }
        return result;
    }

    private Optional<BalanceSheetMetric> findMetric(
            BalanceSheetMetricDefinition definition,
            JsonNode companyFacts,
            AnnualFiling latest10K
    ) {
        for (String concept : definition.usGaapConcepts()) {
            Optional<JsonNode> fact = matchingFact(companyFacts, concept, latest10K);
            if (fact.isPresent()) {
                JsonNode node = fact.get();
                return Optional.of(new BalanceSheetMetric(
                        definition.key(),
                        definition.label(),
                        definition.section(),
                        concept,
                        node.path("val").decimalValue(),
                        "USD",
                        node.path("end").asText(latest10K.reportDate()),
                        node.path("filed").asText(latest10K.filingDate()),
                        node.path("accn").asText(latest10K.accessionNumber())
                ));
            }
        }
        return Optional.empty();
    }

    private Optional<BalanceSheetMetric> deriveMetric(
            BalanceSheetMetricDefinition definition,
            Map<String, BalanceSheetMetric> metricsByKey,
            AnnualFiling latest10K
    ) {
        return switch (definition.key()) {
            case "cash_and_short_term_investments" -> sum(definition, latest10K, metricsByKey,
                    "cash_and_cash_equivalents",
                    "short_term_investments");
            case "other_current_assets" -> residual(definition, latest10K, metricsByKey,
                    "current_assets",
                    "cash_and_cash_equivalents",
                    "short_term_investments",
                    "receivables",
                    "inventory");
            case "non_current_assets" -> subtract(definition, latest10K, metricsByKey,
                    "total_assets",
                    "current_assets");
            case "other_non_current_assets" -> residual(definition, latest10K, metricsByKey,
                    "non_current_assets",
                    "ppe",
                    "goodwill",
                    "intangible_assets",
                    "long_term_investments",
                    "tax_assets");
            case "non_current_liabilities" -> subtract(definition, latest10K, metricsByKey,
                    "total_liabilities",
                    "current_liabilities");
            case "other_current_liabilities" -> residual(definition, latest10K, metricsByKey,
                    "current_liabilities",
                    "accounts_payable",
                    "short_term_debt",
                    "tax_payables",
                    "current_deferred_revenue");
            case "other_non_current_liabilities" -> residual(definition, latest10K, metricsByKey,
                    "non_current_liabilities",
                    "long_term_debt",
                    "non_current_deferred_revenue",
                    "deferred_tax");
            case "other_equity" -> residual(definition, latest10K, metricsByKey,
                    "total_equity",
                    "preferred_stock",
                    "common_stock",
                    "retained_earnings",
                    "aoci");
            default -> Optional.empty();
        };
    }

    private Optional<BalanceSheetMetric> sum(
            BalanceSheetMetricDefinition definition,
            AnnualFiling latest10K,
            Map<String, BalanceSheetMetric> metricsByKey,
            String firstKey,
            String secondKey
    ) {
        BalanceSheetMetric first = metricsByKey.get(firstKey);
        BalanceSheetMetric second = metricsByKey.get(secondKey);
        if (first == null || second == null) {
            return Optional.empty();
        }
        return Optional.of(derived(definition, latest10K, first.value().add(second.value()), firstKey + "+" + secondKey));
    }

    private Optional<BalanceSheetMetric> subtract(
            BalanceSheetMetricDefinition definition,
            AnnualFiling latest10K,
            Map<String, BalanceSheetMetric> metricsByKey,
            String minuendKey,
            String subtrahendKey
    ) {
        BalanceSheetMetric minuend = metricsByKey.get(minuendKey);
        BalanceSheetMetric subtrahend = metricsByKey.get(subtrahendKey);
        if (minuend == null || subtrahend == null) {
            return Optional.empty();
        }
        return Optional.of(derived(
                definition,
                latest10K,
                minuend.value().subtract(subtrahend.value()),
                minuendKey + "-" + subtrahendKey
        ));
    }

    private Optional<BalanceSheetMetric> residual(
            BalanceSheetMetricDefinition definition,
            AnnualFiling latest10K,
            Map<String, BalanceSheetMetric> metricsByKey,
            String totalKey,
            String... componentKeys
    ) {
        BalanceSheetMetric total = metricsByKey.get(totalKey);
        if (total == null) {
            return Optional.empty();
        }

        BigDecimal value = total.value();
        for (String componentKey : componentKeys) {
            BalanceSheetMetric component = metricsByKey.get(componentKey);
            if (component != null) {
                value = value.subtract(component.value());
            }
        }
        return Optional.of(derived(
                definition,
                latest10K,
                value,
                totalKey + "-components"
        ));
    }

    private BalanceSheetMetric derived(
            BalanceSheetMetricDefinition definition,
            AnnualFiling latest10K,
            BigDecimal value,
            String formula
    ) {
        return new BalanceSheetMetric(
                definition.key(),
                definition.label(),
                definition.section(),
                "derived:" + formula,
                value,
                "USD",
                latest10K.reportDate(),
                latest10K.filingDate(),
                latest10K.accessionNumber()
        );
    }

    private Optional<JsonNode> matchingFact(JsonNode companyFacts, String concept, AnnualFiling latest10K) {
        JsonNode facts = companyFacts.path("facts").path("us-gaap").path(concept).path("units").path("USD");
        if (!facts.isArray()) {
            return Optional.empty();
        }

        List<JsonNode> matches = new ArrayList<>();
        for (JsonNode fact : facts) {
            if (latest10K.accessionNumber().equals(fact.path("accn").asText())
                    && fact.path("val").isNumber()) {
                matches.add(fact);
            }
        }

        return matches.stream()
                .filter(fact -> latest10K.reportDate().equals(fact.path("end").asText()))
                .max(Comparator.comparing(fact -> fact.path("filed").asText("")))
                .or(() -> matches.stream()
                        .max(Comparator
                                .comparing((JsonNode fact) -> fact.path("end").asText(""))
                                .thenComparing(fact -> fact.path("filed").asText(""))));
    }

    private AnnualFiling latest10K(JsonNode submissions) {
        List<AnnualFiling> filings = latest10Ks(submissions, 1);
        if (filings.isEmpty()) {
            throw new IllegalArgumentException("No recent 10-K found in submissions data");
        }
        return filings.getFirst();
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

    private static BalanceSheetMetricDefinition metric(String key, String label, String section, String... concepts) {
        return new BalanceSheetMetricDefinition(key, label, section, List.of(concepts));
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
