# Spring IoC as the expected composition direction

The learner states that the eventual application may use Spring Boot or plain Spring Framework IoC and asks for proactive feedback when their design complicates IoC practices. They prefer explicit constructor dependencies for the loader's three statement services. Future reviews should distinguish stable dependencies from request-specific inputs and discuss concrete bean-lifecycle and composition implications.

A custom loader factory remains one implementation option, not a checkpoint acceptance requirement. Reusable constructor-injected collaborators accepting ticker and annualPeriods as method inputs are another option; no refactor has yet been selected or implemented. This preference refines implementation guidance without changing the learning mission.

The learner subsequently selected the reusable-collaborator approach and requested a shared FinancialStatementRequest record at the tool, loader, and service APIs. Codex supplied the refactor, preserving the loader's three explicit service constructor dependencies. The learner explicitly wants to resume their outstanding checkpoint A test work and implement section B themselves; the supplied refactor must not be treated as demonstrated implementation mastery.
