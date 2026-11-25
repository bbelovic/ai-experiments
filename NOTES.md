# Teaching notes

## Starting point
- User explicitly requested the teach skill after discussing an enterprise AI job posting.
- Goal and motivation were supplied in the conversation before MISSION.md was created.
- Existing knowledge: Spring Framework and Spring Boot. Spring AI is new.
- AI concept knowledge has not been assessed. Do not assume either mastery or misconceptions.
- English is the working language; the job posting was in Slovak.
- The learner selected their existing OpenAI account for session 4. Model ID and API project access have not been verified. Do not request credentials in chat.
- Current provider direction (2026-11-11): Gemini Developer API after the learner reported an OpenAI API billing obstacle. Lesson 4 now includes Gemini starter/configuration replacement steps; original OpenAI instructions remain a collapsed alternative. Application changes are left to the learner. Free-tier eligibility, quotas, and available model still need checking in AI Studio.

## Session approach
- Target 20-35 minutes per session as an adjustable starting assumption, not a user commitment.
- Begin with two recall questions from prior sessions; explain one concept; apply it to this repository; finish with a short explanation from the learner.
- Learner attempts predictions and small implementations before the tutor supplies a completed solution.
- Keep the first three lessons dependency-free and usable offline. External reading links need internet.
- In-browser answers are temporary and are not saved or transmitted. The learner shares answers in chat for feedback.
- Create learning records for demonstrated understanding or disclosed prior knowledge, never for merely opening or completing a lesson.
- Add GLOSSARY.md terms only after the learner demonstrates understanding.
- Prepare later implementation lessons adaptively; the roadmap is not evidence of completed learning.

## Spring IoC design direction (2026-11-18)

- The final application may use Spring Boot or plain Spring Framework IoC. Treat this as the expected composition direction, without requiring an immediate framework migration.
- The learner wants the loader's three statement-service dependencies to remain explicit constructor parameters. Preserve constructor injection for stable collaborators.
- Proactively flag designs that complicate IoC: request-specific ticker/annualPeriods in singleton constructor state, mutable per-request fields on shared beans, hidden dependency construction inside business operations, service-locator lookups, circular dependencies, and unnecessary bean-scope/factory machinery. Explain concrete tradeoffs before proposing changes.
- The learner selected reusable constructor-injected services and an immutable FinancialStatementRequest record passed to operation methods. Codex implemented this API refactor at their request; the custom loader factory has been removed. No Spring bean annotations or provider integration were added as part of this refactor.
- For checkpoint A, assess validation, normalized argument forwarding, controlled results, missing-data preservation, and failure propagation regardless of composition approach. Do not require a production factory solely to satisfy the original lesson wording. Lesson 0005's factory exercise and earlier review notes describe one approach and must be read with this update.
- Keep unit tests independent of a Spring context where plain constructor composition suffices. Do not automatically add Spring annotations or redesign application code when only a review was requested.

## Repository constraints
- Preserve existing local modifications: README.md, run.sh, the DividendWatchBrowserLogin test, and untracked UNP JSON samples.
- No production code was changed to prepare the teaching materials.
- Current baseline: Java 25; Maven parent with ai-tooling and chat-client modules. Existing sources and fixtures are under ai-tooling/src; chat-client now has a Gemini starter, ChatClient bean, and AiRunner. Its AI BOM is currently 2.0.0, while the original lesson reference was 2.0.2.
- Prior verification: 49 tests passed and 3 browser-dependent tests skipped outside the sandbox; Mockito self-attachment failed inside the sandbox.
- Before implementation sessions, verify compatible supported Spring Boot / Spring AI versions and pin them. Do not copy snippets from a different release line.
- On 2026-11-10 the learner identified the filing parser/chunk classes as removal candidates. Neither financial-data loader references that subsystem; its callers are parser tests. Do not build teaching exercises or planned retrieval work around retaining those classes. No removal was requested or performed in this teaching revision.
- Lesson 0003 now uses self-contained synthetic research passages. Session 8 will establish an independent document corpus with source metadata. Keep narrative-document retrieval separate from the structured statement tools; document acquisition/ingestion will be selected during implementation.

## Deferred implementation items: EdgarDataLoaderTool

Recorded on 2026-11-09 at the learner's request. Revisit during implementation sessions; these are deferred tasks, not prerequisites for continuing the concept lessons.

Verified by source inspection: getFinancialStatements now returns FinancialStatements via return dataLoader.load(), and years has been renamed to annualPeriods throughout the wrapper.

- [x] Session 5: Document annualPeriods as the requested number of most recent annual filing periods, not calendar-year selection; require a positive count and explain that fewer periods may be available. The arbitrary four-period upper bound was removed at the learner's request on 2026-11-25.
- [x] Session 5: Make the tool's loader replaceable through constructor injection; request-specific ticker and period settings travel in FinancialStatementRequest. Missing test cases remain the learner's task.
- [ ] Sessions 4-5: Move the SEC User-Agent from the hardcoded placeholder to external configuration.
- [ ] Sessions 5-7: Define how the tool adapter exposes validation errors, unknown tickers, and upstream failures. Preserve unavailable metrics as null within successful results; do not convert an operation failure into successful empty data.
- [ ] Sessions 5-7: Register the wrapper as a Spring AI tool and expose it through MCP using the selected compatible release. Verify that the result reaches the client.
- [ ] Session 11: Enforce authorization at the server/service boundary when protected data is introduced; do not rely solely on host-side checks.

## Continuation
Read MISSION.md, RESOURCES.md, NOTES.md, GLOSSARY.md, recent learning records, and recent lessons. Lessons 1-3 concepts and lesson 4's model-request implementation/response inspection have been demonstrated. The next prepared lesson is 0005; start with checkpoint A. Use reference/session-roadmap.html for the planned progression.

## Session 4 preparation
- Prepared lessons/0004-first-spring-ai-request.html with a guided coding exercise, staged hints, an interactive quiz, and explicit acceptance checks.
- At the learner's request, the practice location is now the chat-client Maven module. Codex prepared the parent/child POMs and moved all existing src content into ai-tooling/src. The learner adds the Spring AI BOM, OpenAI starter, configuration, and application code in chat-client. No cross-module dependency is needed for session 4.
- Setup reference: Boot 4.1.2, Spring AI 2.0.2, Java 25, OpenAI model starter. Versions checked against official documentation on 2026-11-10. Boot dependency management and the run plugin are scoped to chat-client; tooling dependency versions remain local to ai-tooling.
- Compile before launching. Two intentional launches compare a supported supplier-risk question against an unsupported debt-impact question using the same fixed synthetic passage.
- This supplies context manually; it does not implement retrieval, tools, or MCP. No dependence on legacy filing classes.
- No provider request was made while preparing the lesson or module setup. Application source contents were preserved during relocation. The root launcher now uses the ai-tooling artifact; module structure and commands are documented in README.md.
- Module setup verification: mvn -o package succeeded for the parent and both modules outside the sandbox (Mockito requires agent attachment). Existing suite: 49 passed, 3 browser-dependent tests skipped. All 40 moved source/resource files matched their pre-move SHA-256 hashes; teaching HTML local links and launcher shell syntax passed checks.

## Session 4 implementation review (2026-11-11)
- User supplied successful outputs from gemini-3.1-flash-lite for both synthetic questions; provider access is demonstrated for those runs. No need to request another live call just to repeat this evidence.
- Two calls occur in one launch, intentionally comparing independent questions. Accept this variation on the lesson's two-launch suggestion; no chat-memory advisor is configured in the reviewed code.
- Review findings and demonstrated implementation skills are recorded in learning record 0013. Leave fixes to the learner unless they ask for implementation help.
- The learner explained why to retain ChatResponse (learning record 0014); other recall prompts can be revisited through later practice.

## Session 5 preparation
- Prepared lessons/0005-a-reliable-financial-tool.html. Current Checkpoint A: use the supplied request-record APIs and injected loader; finish the remaining behavior tests. Checkpoint B: add Spring AI annotations and verify a local JSON callback. The original factory exercise has been superseded by the agreed IoC design.
- Keep the existing two-module arrangement. Work in ai-tooling; no chat-client-to-tooling dependency, model request, or MCP endpoint is needed yet.
- Match the actual existing Spring AI 2.0.0 line for the tooling model library. ToolCallbacks/ToolCallback/ToolDefinition signatures and annotation locations were checked against cached 2.0.0 classes; current reference documentation was consulted for concepts.
- Record constructor: reject blank ticker/nonpositive periods and normalize ticker. Tool: reject null request and delegate; no additional upper bound on the positive integer period count. External User-Agent wiring and client-facing error mapping remain for application/server integration. Authorization remains session 11.

## API refactor and exact resume point (2026-11-18)
- Supplied by Codex at the learner's request: FinancialStatementRequest; load(request) on FinancialDataLoader; three EDGAR service methods taking the record; stable service-only constructor dependencies on EdgarDataLoader; no request values held on services. The existing CLI has been adapted.
- DividendWatchDataLoader implements the same request API and caps returned tables to the requested number of leading periods, preserving null/zero values. Its CLI requests Integer.MAX_VALUE to retain its prior all-scraped-period output. Browser scraping itself still visits the source's normal tables.
- Existing tests were adapted to the signatures, not expanded to solve the learner's tool-test gaps. Separate regression checks cover Dividend Watch limiting and reuse of the cash-flow service for different period requests.
- Tooling suite: 63 tests, 60 passed and 3 browser tests skipped; no live SEC/model calls.
- Current learner work in EdgarDataLoaderToolTest: normalized request forwarding (include surrounding whitespace), upstream error propagation, and missing request. On 2026-11-23 the learner correctly challenged the value of a separate fewer-periods wrapper test; it is no longer required for this direct delegate. Period-selection coverage belongs where filing selection or result transformation occurs. Then continue B with JSON shaped as {"request":{"ticker":"AAPL","annualPeriods":2}} and inspect the generated schema.
- When Spring wiring is introduced, explicitly select the EDGAR FinancialDataLoader implementation if multiple provider loaders are beans; avoid ambiguous interface injection. Actual bean composition is deferred.

## Checkpoint A review conclusion (2026-11-23)
- All 13 focused tool tests passed in the latest reviewed revision. Normalization/delegation, invalid inputs, upstream failure, and null request now have coverage; proceed to checkpoint B.
- The learner correctly challenged the tutor's claim that deleting only the null guard would pass the current test. The next request.annualPeriods() access would throw NullPointerException and fail the exact IllegalArgumentException assertion. A fail-fast loader is an optional way to state no-delegation more explicitly, not a completion blocker. See the correction in learning record 0020.

## Checkpoint B review status (2026-11-24)
- Dependency, parameter retention, annotated tool, successful nested callback invocation, schema structure/required fields, and result JSON equality are implemented. JSON assertions were supplied by Codex at the learner's request, not independently demonstrated mastery.
- After discussion of duplicate coverage, recommend one invalid-record callback test, such as blank ticker, to exercise rejection across the JSON conversion boundary. Do not require the whole validation matrix at every layer. The former annualPeriods=5 rejection case is obsolete following removal of the arbitrary upper bound on 2026-11-25.
- Field-level descriptions are a possible refinement, not an additional completion gate.

## Checkpoint B review conclusion (2026-11-25)
- The learner added testToolCallbackWithInvalidRequestData: nested JSON with an empty ticker is rejected during record construction, with an assertion on the wrapped IllegalArgumentException root cause and message.
- All 15 focused EdgarDataLoaderToolTest cases passed offline. Checkpoint B's implementation target is complete; an explicit verifyNoInteractions assertion on the invalid-input loader is optional clarification, not another completion gate. No additional validation matrix is required.
- Before moving to MCP, revisit the lesson's recall prompts: why a callback needs no model, what the local test proves, and which extra boundary MCP introduces. Do not infer conceptual mastery solely from passing tests.
- Closing discussion: learner correctly placed Java tool execution in the MCP server application and anticipated protocol timeout/lost-response failures. Callback independence and the precise guarantees of local tests were refined with feedback; revisit through practice. Lesson 5 is complete; next prepare session 6, exposing and invoking the tool through MCP without a model first.

## Period-count policy update (2026-11-25)
- The learner challenged the justification for the four-period maximum and requested its removal. Keep only annualPeriods >= 1 in FinancialStatementRequest; the tool retains its null-request guard. Any future operational cap needs an explicit requirement informed by response size or resource usage.
- Codex implemented the requested change and updated the tests and lesson. Positive-count tests now include 1, 5, and Integer.MAX_VALUE; the successful JSON callback requests 5. Historical learning records retain their original review context with a supersession note for the removed cap.
- Verification: all 17 focused EdgarDataLoaderToolTest cases passed offline outside the sandbox (Mockito agent attachment is restricted inside it); no model or SEC requests. git diff --check passed.
