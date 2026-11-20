# Testing the tool boundary through an injected factory

On 2026-11-18, the learner introduced a FinancialDataLoaderFactory constructor dependency in EdgarDataLoaderTool and a parameterized test invoking the tool with null/empty ticker inputs. Validation remains in the tool before factory delegation, which matches checkpoint A's boundary design. The learner asked whether validation should move into the factory; guidance is to retain the tool contract checks and keep construction as the factory's responsibility.

The current test asserts the validation exception but does not yet prove the factory was never called. It also covers null/empty rather than whitespace-only strings. Next steps are an explicit no-factory-call assertion (or fail-fast test factory), whitespace cases, and a successful normalization/delegation case. The synthetic fixture's source label and period keys also need alignment with the exercise. No application or test code was changed in this structural review.
