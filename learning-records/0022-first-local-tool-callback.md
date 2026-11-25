# First local tool callback invocation

Historical review: checkpoint B was subsequently completed (learning record 0023). The four-period cap and any requirement to reject annualPeriods=5 were removed on 2026-11-25 (learning record 0024).

On 2026-11-24, the learner created a ToolCallback from the annotated tool, asserted one callback and its operation name, and successfully invoked it with nested request JSON. All 14 focused tests passed with fixture data and no model or SEC requests. This demonstrates the method-to-callback wiring and record input conversion for a valid case.

Checkpoint B remains partial: the callback test only asserts nonempty output, not its JSON contents; it does not yet assert the generated nested schema/required fields or callback rejection before loader execution for excessive periods and invalid record data. Next practice should add those contract assertions rather than treating a nonempty string as evidence that serialization preserved null and zero values.

Subsequent implementation now asserts the essential schema paths and required fields and compares the full returned JSON tree with the fixture, preserving zero and null. These assertions were supplied by Codex at the learner's request; the learner extracted schema checks into a helper. The remaining required implementation work is callback rejection for nested annualPeriods=5 and invalid record data, with verification that the loader is not invoked. Existing validation tests call the Java method directly and do not cover those callback paths.

The learner questioned duplicate validation coverage across layers. Review guidance was refined: recommend one invalid-record JSON callback case to exercise deserialization/rejection, while the excessive-period callback case is optional because the rule already has direct Java coverage. Do not repeat the full validation matrix at every layer. The earlier requirement for both callback cases is superseded by this narrower recommendation.
