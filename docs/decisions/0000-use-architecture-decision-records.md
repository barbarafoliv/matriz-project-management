# 0000 - Use Architecture Decision Records

## Status

Accepted

## Context

The project needs a simple, traceable and versioned way to record important architectural decisions.

## Decision

Architecture decisions will be recorded as Architecture Decision Records (ADRs) in Markdown files, stored in `docs/decisions`.

- Each ADR is numbered sequentially (`NNNN-short-title.md`) and has the sections Status, Context, Decision and Consequences.
- An accepted ADR is not rewritten. If a decision changes, a new ADR supersedes it.

## Consequences

- Decisions are documented and versioned in the repository.
- The context and trade-offs behind each decision are preserved.
- New team members can understand the technical reasoning more easily.
- Writing and maintaining ADRs takes some extra effort.
