# Java enterprise AI resources

Sources initially reviewed on 2026-11-04; session 4 setup sources checked on 2026-11-10. The reviewed Spring AI reference identifies itself as 2.0.2 and supports Boot 4.1.x. Lesson 4 pins Boot 4.1.2 and AI 2.0.2 with Java 25. Recheck before later upgrades; do not mix release-line configuration.

## Knowledge

- [Spring AI: Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html)
  Primary source for the model-request/application-execution boundary and Java tool integration. Use for lessons 0001 and later tool implementation.
- [MCP: Architecture overview](https://modelcontextprotocol.io/docs/learn/architecture)
  Protocol project's explanation of hosts, clients, servers, and capability exchange. Use for lesson 0002; protocol details may evolve independently of Spring AI releases.
- [Spring AI: MCP integration](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html)
  Java integration and supported client/server arrangements. Use when mapping protocol concepts to Spring components.
- [Spring AI: Retrieval Augmented Generation](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
  Primary source for retrieving documents into model context and advisor integration. Use for lesson 0003 and the retrieval implementation.
- [Spring AI: PGvector](https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html)
  Vector store configuration and metadata filtering. Use after a manually inspectable retrieval baseline exists.
- [Spring AI: MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
  Server setup and security boundaries. Use for transport selection and the authentication milestone; network transport does not automatically supply authorization.

### First request: implementation sources

- [Spring AI: Getting Started](https://docs.spring.io/spring-ai/reference/getting-started.html)
  Compatibility matrix and BOM management. Use for the pinned session 4 practice POM.
- [Spring Boot: System Requirements](https://docs.spring.io/spring-boot/system-requirements.html)
  Java and Maven compatibility for Boot 4.1.2. Use to check the Java 25 setup.
- [Spring AI: ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
  Builder injection, message construction, and ChatResponse access. Recommended primary reading for session 4.
- [Spring AI: OpenAI Chat](https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html)
  Starter, model selection, API-key binding, retry configuration, and model-specific options. Use for session 4; do not assume parameter support transfers between models.
- [Spring AI: Response Metadata](https://docs.spring.io/spring-ai/reference/api/usage-handling.html)
  Reported usage and response metadata. Use to inspect one response rather than make another request to obtain counts.
- [OpenAI: Developer Quickstart](https://developers.openai.com/api/docs/quickstart)
  Official API setup and environment-based credentials. No account access or entitlement was verified while preparing the lesson.
- [OpenAI: Chat Completions](https://developers.openai.com/api/reference/resources/chat/subresources/completions/methods/create)
  Provider endpoint and model-dependent parameters behind the lesson's Spring AI integration; also notes OpenAI's Responses recommendation for new direct API projects.
- [OpenAI: Model Catalog](https://developers.openai.com/api/docs/models)
  Check a model's supported endpoint and parameters, then select an exact ID available to the learner's API project. Session 4 leaves the model explicit via OPENAI_MODEL rather than guessing account access.
- [Spring Boot: Running with Maven](https://docs.spring.io/spring-boot/maven-plugin/run.html)
  Launch the isolated practice project using spring-boot:run.

## Wisdom (Communities)

- [Spring AI GitHub Discussions](https://github.com/spring-projects/spring-ai/discussions)
  Project community for concrete design questions and minimal reproductions. Use after forming an explanation or reproducing a problem locally; check current discussion categories before posting.

## Gaps

- Current session 4 route: Gemini Developer API after an OpenAI billing obstacle. Exact model, free-tier eligibility, and API access remain to be checked locally; credentials must not be shared in chat.
- Add identity-provider and cloud documentation after choosing a deployment target.
- No model-provider account, API key, paid subscription, or cloud deployment is needed for lessons 0001-0003.

## Gemini route (checked 2026-11-11)

- [Spring AI: Google GenAI Chat](https://docs.spring.io/spring-ai/reference/api/chat/google-genai-chat.html)
  Starter and Gemini Developer API key/model properties; distinguish this route from Vertex AI authentication.
- [Gemini API billing](https://ai.google.dev/gemini-api/docs/billing)
  Free-tier eligibility, limited model access, quotas, and the separate upgrade to paid billing. Check the learner's actual project in AI Studio.
- [AI Studio API keys](https://aistudio.google.com/api-keys)
  Local credential setup; never share the key with the tutor or commit it to the repository.
