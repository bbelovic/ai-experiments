# Per-call inputs in a Java tool wrapper

On 2026-11-09, the learner implemented a preliminary EdgarDataLoaderTool wrapper accepting ticker and years, rejecting blank tickers and nonpositive year counts before constructing services, and delegating retrieval to EdgarDataLoader. This demonstrates translating the earlier tool-boundary discussion into code and keeping request-specific settings local to each invocation.

Review identified that the void method discards the FinancialStatements result. Next practice should complete the result contract and explain what a caller receives on success, missing data, and failure. Framework registration and server-side authorization remain later topics.

Later on 2026-11-09, source inspection confirmed that the learner changed the return type to FinancialStatements, returned dataLoader.load(), and renamed years to annualPeriods throughout the wrapper. The code-level result contract issue is resolved; the learner's explanation of missing-data semantics has not yet been assessed. Remaining implementation tasks are tracked in NOTES.md under Deferred implementation items: EdgarDataLoaderTool.
