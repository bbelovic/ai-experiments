package org.example.statements.cashflow;

import java.util.List;

public enum CashFlowStatementMetricEnumType {
    NET_INCOME("net_income", "Net income", "Operating activities", "USD",
            List.of("NetIncomeLoss")),
    DEPRECIATION_DEPLETION_AND_AMORTIZATION("depreciation_depletion_and_amortization", "Depreciation, depletion & amortization", "Operating activities", "USD",
            List.of("DepreciationDepletionAndAmortization")),
    SHARE_BASED_COMPENSATION("share_based_compensation", "Share-based compensation", "Operating activities", "USD",
            List.of("ShareBasedCompensation")),
    DEFERRED_INCOME_TAX("deferred_income_tax", "Deferred income tax expense/benefit", "Operating activities", "USD",
            List.of("DeferredIncomeTaxExpenseBenefit")),
    OPERATING_CASH_FLOW("operating_cash_flow", "Operating cash flow", "Operating activities", "USD",
            List.of("NetCashProvidedByUsedInOperatingActivities")),
    CAPITAL_EXPENDITURES("capital_expenditures", "Capital expenditures", "Investing activities", "USD",
            List.of("PaymentsToAcquirePropertyPlantAndEquipment")),
    INVESTING_CASH_FLOW("investing_cash_flow", "Investing cash flow", "Investing activities", "USD",
            List.of("NetCashProvidedByUsedInInvestingActivities")),
    DIVIDENDS_PAID("dividends_paid", "Dividends paid", "Financing activities", "USD",
            List.of("PaymentsOfDividends")),
    SHARE_REPURCHASES("share_repurchases", "Common stock repurchases", "Financing activities", "USD",
            List.of("PaymentsForRepurchaseOfCommonStock")),
    LONG_TERM_DEBT_PROCEEDS("long_term_debt_proceeds", "Long-term debt proceeds", "Financing activities", "USD",
            List.of("ProceedsFromIssuanceOfLongTermDebt")),
    LONG_TERM_DEBT_REPAYMENTS("long_term_debt_repayments", "Long-term debt repayments", "Financing activities", "USD",
            List.of("RepaymentsOfLongTermDebt")),
    FINANCING_CASH_FLOW("financing_cash_flow", "Financing cash flow", "Financing activities", "USD",
            List.of("NetCashProvidedByUsedInFinancingActivities")),
    NET_CHANGE_IN_CASH_INCLUDING_RESTRICTED_CASH("net_change_in_cash_including_restricted_cash", "Net change in cash, cash equivalents & restricted cash", "Cash reconciliation", "USD",
            List.of("CashCashEquivalentsRestrictedCashAndRestrictedCashEquivalentsPeriodIncreaseDecreaseIncludingExchangeRateEffect")),
    FREE_CASH_FLOW("free_cash_flow", "Free cash flow", "Supplemental", "USD",
            List.of());

    private final String key;
    private final String label;
    private final String section;
    private final String unit;
    private final List<String> usGAAPConcepts;

    CashFlowStatementMetricEnumType(String key, String label, String section, String unit, List<String> usGAAPConcepts) {
        this.key = key;
        this.label = label;
        this.section = section;
        this.unit = unit;
        this.usGAAPConcepts = usGAAPConcepts;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getSection() {
        return section;
    }

    public String getUnit() {
        return unit;
    }

    public List<String> getUsGAAPConcepts() {
        return usGAAPConcepts;
    }
}
