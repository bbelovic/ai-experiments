package org.example.loader;

import org.example.statements.FinancialStatements;
import org.example.statements.FinancialStatementRequest;

public interface FinancialDataLoader {
    FinancialStatements load(FinancialStatementRequest request);
}
