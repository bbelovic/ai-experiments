package org.example.tools;

import org.example.loader.FinancialDataLoader;
import org.example.statements.FinancialStatementRequest;
import org.example.statements.FinancialStatements;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class EdgarDataLoaderTool {

    private final FinancialDataLoader loader;

    public EdgarDataLoaderTool(FinancialDataLoader loader) {
        this.loader = loader;
    }

    @Tool(description = """
            Provides financial statement data for given stock ticker over the requested number of most recent annual periods.

            Returned object contains cash-flow, income and balance sheet statements.
            Returned financial metrics values are numeric strings or null if value is not present or can not be calculated.
            Tool might or might not return these statements for all requested periods if these are not available.""")
    public FinancialStatements getFinancialStatements(@ToolParam(description = """
            Wraps stock ticker being requested (can't be null or blank) and number of annual periods being requested.

            Number of annual periods must be positive. Fewer periods may be returned if fewer are available.
            """) FinancialStatementRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Financial statement request is required");
        }
        return loader.load(request);
    }
}
