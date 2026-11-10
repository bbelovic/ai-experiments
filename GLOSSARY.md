# AI application glossary

Terms demonstrated through the learner's explanations and exercises. Extend this file as understanding is demonstrated.

## Terms

**Retrieval**:
Selecting evidence from a source so an application can supply it as context for an answer.

**Generation**:
Producing an answer from the model's inputs, including any supplied evidence; relevant evidence alone does not guarantee an accurate answer.

**RAG (retrieval augmented generation)**:
An application pattern that retrieves evidence and supplies it as context for generation.
_Avoid_: Treating RAG as a communication protocol.

**MCP (Model Context Protocol)**:
A protocol through which a host application's MCP client communicates with an MCP server exposing capabilities such as tools.
_Avoid_: Treating MCP as a model or as a synonym for RAG.
