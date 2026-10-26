# 0001 - Use a Modular Monolith

## Status

Accepted

## Context

Matriz is built by a small team on a short timeline. The MVP is used by small teams in one shared workspace, so there is no need for independent scaling. The application must still keep users, projects and tasks clearly separated, and respect access and GDPR rules (NFR-02, FR-14).

## Decision

Matriz will be built as a modular monolith: one application, deployed as a single unit, split into modules with clear boundaries.

- Each module owns its business logic and data, and other modules use it only through its public interface.
- The initial modules follow the requirements:
  - **Accounts:** sign up, sign in, sign out and account deletion (FR-01, FR-14).
  - **Projects:** projects and their members (FR-02, FR-03).
  - **Tasks:** tasks, statuses, board, "My tasks" and late tasks (FR-04 to FR-08).

## Alternatives considered

- **Microservices:** independent scaling and deployment, but too much operational complexity for a small team and an MVP.
- **Monolith without modules:** the simplest start, but boundaries erode over time and the code becomes harder to change.

## Consequences

- One codebase, one build and one deployment keep development and hosting simple.
- Clear module boundaries make the code easier to understand, test and change.
- A module can later be extracted into a separate service if needed.
- The boundaries must be respected by discipline and code review, since nothing in the deployment enforces them.
