# MCP boundaries and model independence

On 2026-11-09, the learner correctly described the host containing an MCP client, the client communicating with a server through the standardized protocol, and the server invoking tool implementation through ordinary Java calls in this project's proposed design. They also correctly explained that the MCP server can operate without a model and that the underlying capability can be invoked without MCP.

Their CLI example illustrates reuse of an underlying operation. Clarify that a tool need not be a CLI application: here a Java service method can be wrapped for MCP while also being called directly. Future sessions can build on the demonstrated architectural boundary; the server may also delegate to a separate API or process rather than containing all implementation logic.
