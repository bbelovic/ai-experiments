# First tool annotations and framework dependency

Historical review: the four-period limit described below was removed at the learner's request on 2026-11-25. The current contract requires only a positive period count; see learning record 0024.

The learner added spring-ai-model 2.0.0 and annotated the financial operation with Tool and its request parameter with ToolParam. All 13 existing tool tests passed after compilation; these tests still invoke Java directly and do not yet verify the Spring AI callback or schema.

Remaining checkpoint B work: retain method parameter names (the compiled tool currently lacks MethodParameters), fix or remove the unused misspelled spring.ai.model.verion property, describe the 1..4 tool limit and formatted-string/null result semantics, and add the planned local schema/callback tests. No source edits or live model/SEC requests were made during this review.

On 2026-11-24, the learner added maven.compiler.parameters=true in properties, fixed and referenced spring.ai.model.version, and expanded descriptions with the period bound and numeric-string/null result semantics. Compilation passed and javap confirmed the retained method parameter name request. Suggest placing result semantics in Tool rather than the input ToolParam description; this is an organization improvement, not a blocker. Next work is the local callback/schema tests from section B.
