package org.example.tools;

import org.example.loader.FinancialDataLoader;
import org.example.statements.FinancialStatementRequest;
import org.example.statements.FinancialStatements;

public class EdgarDataLoaderTool {

    private final FinancialDataLoader loader;

    public EdgarDataLoaderTool(FinancialDataLoader loader) {
        this.loader = loader;
    }

    /** Returns up to four recent annual periods; unavailable metric values remain null. */
    public FinancialStatements getFinancialStatements(FinancialStatementRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Financial statement request is required");
        }
        if (request.annualPeriods() > 4) {
            throw new IllegalArgumentException("Invalid annualPeriods value: [%d]".formatted(request.annualPeriods()));
        }
        return loader.load(request);
    }
}
