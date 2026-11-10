package org.example.statements.income;

import java.util.List;

public enum IncomeStatementMetricEnumType {
    REVENUE("revenue", "Revenue", "Revenue", "USD",
            List.of("RevenueFromContractWithCustomerExcludingAssessedTax", "Revenues", "SalesRevenueNet")),
    COST_OF_REVENUE("cost_of_revenue", "Cost of revenue", "Revenue", "USD",
            List.of("CostOfRevenue", "CostOfGoodsAndServicesSold", "CostOfGoodsSold")),
    GROSS_PROFIT("gross_profit", "Gross profit", "Revenue", "USD",
            List.of("GrossProfit")),
    RESEARCH_AND_DEVELOPMENT("research_and_development", "Research & development", "Operating expenses", "USD",
            List.of("ResearchAndDevelopmentExpense")),
    SELLING_GENERAL_AND_ADMINISTRATIVE("selling_general_and_administrative", "Selling, general & administrative", "Operating expenses", "USD",
            List.of("SellingGeneralAndAdministrativeExpense", "SellingAndMarketingExpense", "GeneralAndAdministrativeExpense")),
    OPERATING_EXPENSES("operating_expenses", "Operating expenses", "Operating expenses", "USD",
            List.of("OperatingExpenses")),
    OPERATING_INCOME("operating_income", "Operating income", "Income", "USD",
            List.of("OperatingIncomeLoss")),
    NON_OPERATING_INCOME_EXPENSE("non_operating_income_expense", "Non-operating income/expense", "Income", "USD",
            List.of("NonoperatingIncomeExpense", "OtherNonoperatingIncomeExpense")),
    PRETAX_INCOME("pretax_income", "Pretax income", "Income", "USD",
            List.of("IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest", "IncomeLossFromContinuingOperationsBeforeIncomeTaxesMinorityInterestAndIncomeLossFromEquityMethodInvestments")),
    INCOME_TAX_EXPENSE("income_tax_expense", "Income tax expense", "Income", "USD",
            List.of("IncomeTaxExpenseBenefit")),
    NET_INCOME("net_income", "Net income", "Income", "USD",
            List.of("NetIncomeLoss")),
    BASIC_EPS("basic_eps", "Basic EPS", "Per share", "USD/shares",
            List.of("EarningsPerShareBasic")),
    DILUTED_EPS("diluted_eps", "Diluted EPS", "Per share", "USD/shares",
            List.of("EarningsPerShareDiluted")),
    BASIC_SHARES("basic_shares", "Basic shares", "Shares", "shares",
            List.of("WeightedAverageNumberOfSharesOutstandingBasic")),
    DILUTED_SHARES("diluted_shares", "Diluted shares", "Shares", "shares",
            List.of("WeightedAverageNumberOfDilutedSharesOutstanding"));

    private final String key;
    private final String label;
    private final String section;
    private final String unit;
    private final List<String> usGAAPConcepts;

    IncomeStatementMetricEnumType(String key, String label, String section, String unit, List<String> usGAAPConcepts) {
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
