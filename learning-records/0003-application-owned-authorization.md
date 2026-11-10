# Application-owned authorization

On 2026-11-08, the learner correctly identified permission checking as an application responsibility before tool execution. This builds on their demonstrated understanding of application-owned execution and input validation.

The next distinction to reinforce is that validation checks whether inputs are acceptable, while authorization checks whether the authenticated caller may perform the requested operation on the requested data. The service providing access must enforce authorization even when a calling assistant performs preliminary checks; server-side enforcement across an MCP boundary has not yet been demonstrated.
