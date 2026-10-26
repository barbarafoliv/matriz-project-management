# 0002 - Use PostgreSQL

## Status

Accepted

## Context

Matriz needs a database to store users, projects, members and tasks. The data is relational: users belong to projects, and tasks belong to a project and have at most one assignee. Saved data must not be lost (NFR-03), and users must be able to delete their account and personal data (FR-14). The application is a modular monolith ([ADR 0001](0001-use-modular-monolith.md)).

## Decision

Matriz will use PostgreSQL as its database.

- The whole application uses a single PostgreSQL database.
- Each module owns its own tables, and other modules don't access them directly.

## Alternatives considered

- **MySQL:** also a good relational option, but it offers no clear advantage for this project.
- **SQLite:** simple to set up, but less suited to many users writing at the same time on a server.
- **A document database (e.g. MongoDB):** flexible, but the data is relational and needs consistent links between users, projects and tasks.

## Consequences

- Foreign keys and transactions keep the links between users, projects and tasks consistent.
- PostgreSQL is reliable, open source and widely supported by frameworks and hosting providers.
- Deleting an account and its personal data can be handled with clear rules in the database.
- The database must be set up, backed up and maintained.
