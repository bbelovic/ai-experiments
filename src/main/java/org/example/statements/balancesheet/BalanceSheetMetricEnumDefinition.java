package org.example.statements.balancesheet;

import java.util.List;

public enum BalanceSheetMetricEnumDefinition {
    TOTAL_ASSETS("total_assets", "Total assets", "Assets", List.of("Assets")),
    CURRENT_ASSETS("current_assets", "Current assets", "Assets", List.of("AssetsCurrent")),
    CASH_AND_CASH_EQUIVALENTS("cash_and_cash_equivalents", "Cash & cash equivalents", "Assets",
            List.of("CashAndCashEquivalentsAtCarryingValue", "CashCashEquivalentsRestrictedCashAndRestrictedCashEquivalents")),
    SHORT_TERM_INVESTMENTS("short_term_investments", "Short-term investments", "Assets",
            List.of("ShortTermInvestments", "MarketableSecuritiesCurrent")),
    CASH_AND_SHORT_TERM_INVESTMENTS("cash_and_short_term_investments", "Cash & short-term investments", "Assets", List.of()),
    RECEIVABLES("receivables", "Receivables", "Assets",
            List.of("AccountsReceivableNetCurrent",
                    "AccountsReceivableNet",
                    "ReceivablesNetCurrent")),
    INVENTORY("inventory", "Inventory", "Assets", List.of("InventoryNet")),
    OTHER_CURRENT_ASSETS("other_current_assets", "Other current assets", "Assets", List.of("OtherCurrentAssets")),
    NON_CURRENT_ASSETS("non_current_assets", "Non-current assets", "Assets", List.of()),
    PPE("ppe", "PP&E", "Assets",
            List.of("PropertyPlantAndEquipmentNet",
                    "PropertyPlantAndEquipmentAndFinanceLeaseRightOfUseAssetAfterAccumulatedDepreciationAndAmortization")),
    GOODWILL("goodwill", "Goodwill", "Assets", List.of("Goodwill")),
    INTANGIBLE_ASSETS("intangible_assets", "Intangible assets", "Assets",
            List.of("FiniteLivedIntangibleAssetsNet",
                    "IntangibleAssetsNetExcludingGoodwill",
                    "IntangibleAssetsNetIncludingGoodwill")),
    LONG_TERM_INVESTMENTS("long_term_investments", "Long-term investments", "Assets",
            List.of("MarketableSecuritiesNoncurrent", "LongTermInvestments")),
    TAX_ASSETS("tax_assets", "Tax assets", "Assets",
            List.of("DeferredTaxAssetsNet", "DeferredTaxAssetsNetCurrent", "DeferredTaxAssetsLiabilitiesNet")),
    OTHER_NON_CURRENT_ASSETS("other_non_current_assets", "Other non-current assets", "Assets", List.of("OtherAssetsNoncurrent")),
    TOTAL_LIABILITIES("total_liabilities", "Total liabilities", "Liabilities", List.of("Liabilities")),
    CURRENT_LIABILITIES("current_liabilities", "Current liabilities", "Liabilities", List.of("LiabilitiesCurrent")),
    ACCOUNTS_PAYABLE("accounts_payable", "Accounts payable",
            "Liabilities", List.of("AccountsPayableCurrent")),
    SHORT_TERM_DEBT("short_term_debt", "Short-term debt", "Liabilities",
            List.of("ShortTermBorrowings", "ShortTermDebtCurrent", "LongTermDebtCurrent")),
    TAX_PAYABLES("tax_payables", "Tax payables", "Liabilities", List.of("TaxesPayableCurrent")),
    CURRENT_DEFERRED_REVENUE("current_deferred_revenue", "Deferred revenue", "Liabilities",
            List.of("ContractWithCustomerLiabilityCurrent", "DeferredRevenueCurrent")),
    OTHER_CURRENT_LIABILITIES("other_current_liabilities", "Other current liabilities", "Liabilities", List.of("OtherCurrentLiabilities")),
    NON_CURRENT_LIABILITIES("non_current_liabilities", "Non-current liabilities", "Liabilities", List.of("LiabilitiesNoncurrent")),
    LONG_TERM_DEBT("long_term_debt", "Long-term debt", "Liabilities",
            List.of("LongTermDebtNoncurrent", "LongTermDebtAndFinanceLeaseObligationsNoncurrent")),
    NON_CURRENT_DEFERRED_REVENUE("non_current_deferred_revenue", "Deferred revenue", "Liabilities",
            List.of("ContractWithCustomerLiabilityNoncurrent", "DeferredRevenueNoncurrent")),
    DEFERRED_TAX("deferred_tax", "Deferred tax", "Liabilities",
            List.of("DeferredTaxLiabilitiesNoncurrent", "DeferredTaxLiabilitiesNet")),
    OTHER_NON_CURRENT_LIABILITIES("other_non_current_liabilities", "Other non-current liabilities", "Liabilities", List.of("OtherLiabilitiesNoncurrent")),
    TOTAL_EQUITY("total_equity", "Total equity", "Equity",
            List.of("StockholdersEquity", "StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest")),
    PREFERRED_STOCK("preferred_stock", "Preferred stock", "Equity",
            List.of("PreferredStocksIncludingAdditionalPaidInCapital", "PreferredStockValue")),
    COMMON_STOCK("common_stock", "Common stock", "Equity",
            List.of("CommonStocksIncludingAdditionalPaidInCapital", "CommonStockValue")),
    RETAINED_EARNINGS("retained_earnings", "Retained earnings", "Equity", List.of("RetainedEarningsAccumulatedDeficit")),
    AOCI("aoci", "AOCI", "Equity", List.of("AccumulatedOtherComprehensiveIncomeLossNetOfTax")),
    OTHER_EQUITY("other_equity", "Other equity", "Equity", List.of());

    private final String key;
    private final String label;
    private final String section;
    private final List<String> usGaapConcepts;


    BalanceSheetMetricEnumDefinition(String key, String label, String section, List<String> usGaapConcepts) {
        this.key = key;
        this.label = label;
        this.section = section;
        this.usGaapConcepts = usGaapConcepts;
    }

    public String getLabel() {
        return label;
    }

    public String getKey() {
        return key;
    }

    public String getSection() {
        return section;
    }

    public List<String> getUsGaapConcepts() {
        return usGaapConcepts;
    }
}
