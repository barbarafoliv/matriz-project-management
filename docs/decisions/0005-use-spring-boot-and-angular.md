# 0005 - Use Spring Boot and Angular

## Status

Accepted

## Context

Matriz is a modular monolith ([ADR 0001](0001-use-modular-monolith.md)) with a REST API ([ADR 0003](0003-use-rest-between-clients-and-api.md)) backed by PostgreSQL ([ADR 0002](0002-use-postgresql.md)). The application stack must support server-side sessions ([ADR 0004](0004-use-email-password-with-server-side-sessions.md)), module boundaries, relational data access and a browser-based client. This is an academic project developed by one person.

## Decision

The backend will be built with **Spring Boot** and the frontend with **Angular**.

- Spring Boot serves the REST API, manages sessions, enforces access rules and handles all business logic.
- Angular runs in the browser, consumes the REST API and renders the user interface.
- Sessions and failed sign-in attempts are stored in PostgreSQL, using Spring Session with JDBC. No separate session store is needed.

## Alternatives considered

- **Node.js (Express/NestJS) + React:** familiar in many teams, but Spring Boot's module system, built-in security and JDBC support fit the project requirements more directly.
- **Full-stack framework (e.g. Next.js):** simpler deployment, but blurs the client/server boundary defined in ADR 0003.

## Consequences

- Spring Boot's module system supports the modular monolith structure.
- Spring Security covers authentication, session management, rate-limiting of failed attempts and access control (NFR-02).
- Spring Session with JDBC stores sessions in PostgreSQL, keeping the infrastructure simple — one database, no extra service.
- Angular's component model maps naturally to the board and task views (FR-06, FR-07).
- The project requires JDK and Node.js to be installed locally.
