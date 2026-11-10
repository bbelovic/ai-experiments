# Application-owned tool execution and validation

On 2026-11-08, the learner correctly stated that the calling application executes tools and owns input validation. In a comment above EdgarDataLoader.load(), they identified the statement-loading operation as a tool candidate, proposed ticker and years as inputs, suggested validating empty tickers and positive year counts, and considered a structured error result.

Future teaching can build on this separation of model requests from application execution. Follow up on per-request inputs versus constructor-held state, bounded year counts, distinguishing missing facts from operation failures, and authorization; understanding of authorization has not yet been demonstrated.
