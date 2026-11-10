# Retrieval exercises independent of the legacy parser

On 2026-11-10, the learner disclosed that the filing parser and chunk classes are removal candidates and requested that lesson 0003 reflect this direction. Source inspection showed that neither financial-data loader references the subsystem; references outside it are in parser tests.

Lesson 0003 now uses self-contained synthetic passages with source metadata, and the roadmap's retrieval baseline uses an independent document corpus. This is a teaching implementation constraint, not a change to the mission or evidence of RAG mastery. No application classes were removed.
