# Teaching notes

## Starting point
- User explicitly requested the teach skill after discussing an enterprise AI job posting.
- Goal and motivation were supplied in the conversation before MISSION.md was created.
- Existing knowledge: Spring Framework and Spring Boot. Spring AI is new.
- AI concept knowledge has not been assessed. Do not assume either mastery or misconceptions.
- English is the working language; the job posting was in Slovak.
- The learner selected their existing OpenAI account for session 4. Model ID and API project access have not been verified. Do not request credentials in chat.

## Session approach
- Target 20-35 minutes per session as an adjustable starting assumption, not a user commitment.
- Begin with two recall questions from prior sessions; explain one concept; apply it to this repository; finish with a short explanation from the learner.
- Learner attempts predictions and small implementations before the tutor supplies a completed solution.
- Keep the first three lessons dependency-free and usable offline. External reading links need internet.
- In-browser answers are temporary and are not saved or transmitted. The learner shares answers in chat for feedback.
- Create learning records for demonstrated understanding or disclosed prior knowledge, never for merely opening or completing a lesson.
- Add GLOSSARY.md terms only after the learner demonstrates understanding.
- Prepare later implementation lessons adaptively; the roadmap is not evidence of completed learning.

## Repository constraints
- Preserve existing local modifications: README.md, run.sh, the DividendWatchBrowserLogin test, and untracked UNP JSON samples.
- No production code was changed to prepare the teaching materials.
- Current baseline: Java 25; Maven parent with ai-tooling and chat-client modules. Existing sources and fixtures are under ai-tooling/src; chat-client has Boot build support but no Spring AI dependency or application yet.
- Prior verification: 49 tests passed and 3 browser-dependent tests skipped outside the sandbox; Mockito self-attachment failed inside the sandbox.
- Before implementation sessions, verify compatible supported Spring Boot / Spring AI versions and pin them. Do not copy snippets from a different release line.
- On 2026-11-10 the learner identified the filing parser/chunk classes as removal candidates. Neither financial-data loader references that subsystem; its callers are parser tests. Do not build teaching exercises or planned retrieval work around retaining those classes. No removal was requested or performed in this teaching revision.
- Lesson 0003 now uses self-contained synthetic research passages. Session 8 will establish an independent document corpus with source metadata. Keep narrative-document retrieval separate from the structured statement tools; document acquisition/ingestion will be selected during implementation.

## Deferred implementation items: EdgarDataLoaderTool

Recorded on 2026-11-09 at the learner's request. Revisit during implementation sessions; these are deferred tasks, not prerequisites for continuing the concept lessons.

Verified by source inspection: getFinancialStatements now returns FinancialStatements via return dataLoader.load(), and years has been renamed to annualPeriods throughout the wrapper.

- [ ] Session 5: Document annualPeriods as the requested number of recent annual filing periods, not calendar-year selection; choose and validate an application-defined upper bound. Explain behavior when fewer periods are available.
- [ ] Session 5: Make service/loader construction replaceable, for example through an injected factory, so tool tests can use fixtures without live SEC requests. Preserve per-call ticker and period settings.
- [ ] Sessions 4-5: Move the SEC User-Agent from the hardcoded placeholder to external configuration.
- [ ] Sessions 5-7: Define how the tool adapter exposes validation errors, unknown tickers, and upstream failures. Preserve unavailable metrics as null within successful results; do not convert an operation failure into successful empty data.
- [ ] Sessions 5-7: Register the wrapper as a Spring AI tool and expose it through MCP using the selected compatible release. Verify that the result reaches the client.
- [ ] Session 11: Enforce authorization at the server/service boundary when protected data is introduced; do not rely solely on host-side checks.

## Continuation
Read MISSION.md, RESOURCES.md, NOTES.md, GLOSSARY.md, recent learning records, and recent lessons. Lessons 1-3 objectives have been demonstrated (see learning record 0009); the next prepared lesson is 0004. Use reference/session-roadmap.html for the planned progression.

## Session 4 preparation
- Prepared lessons/0004-first-spring-ai-request.html with a guided coding exercise, staged hints, an interactive quiz, and explicit acceptance checks.
- At the learner's request, the practice location is now the chat-client Maven module. Codex prepared the parent/child POMs and moved all existing src content into ai-tooling/src. The learner adds the Spring AI BOM, OpenAI starter, configuration, and application code in chat-client. No cross-module dependency is needed for session 4.
- Setup reference: Boot 4.1.2, Spring AI 2.0.2, Java 25, OpenAI model starter. Versions checked against official documentation on 2026-11-10. Boot dependency management and the run plugin are scoped to chat-client; tooling dependency versions remain local to ai-tooling.
- Compile before launching. Two intentional launches compare a supported supplier-risk question against an unsupported debt-impact question using the same fixed synthetic passage.
- This supplies context manually; it does not implement retrieval, tools, or MCP. No dependence on legacy filing classes.
- No provider request was made while preparing the lesson or module setup. Application source contents were preserved during relocation. The root launcher now uses the ai-tooling artifact; module structure and commands are documented in README.md.
- Module setup verification: mvn -o package succeeded for the parent and both modules outside the sandbox (Mockito requires agent attachment). Existing suite: 49 passed, 3 browser-dependent tests skipped. All 40 moved source/resource files matched their pre-move SHA-256 hashes; teaching HTML local links and launcher shell syntax passed checks.
