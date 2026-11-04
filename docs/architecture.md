---
title: Architecture
---

# Matriz: Architecture

> Complements [requirements.md](requirements.md), [user-stories.md](user-stories.md) and [constraints.md](constraints.md). Decisions are recorded in the [ADRs](decisions/).

## 1. Context

The MVP covers FR-01 to FR-10, FR-14 to FR-17 and NFR-01 to NFR-03. It is built by a small team on a short timeline, and used by small teams in one shared workspace. There is no need for independent scaling, but access rules (NFR-02) and personal data deletion (FR-14) must be applied consistently.

## 2. Alternatives

| | Monolith | Modular monolith | Microservices |
|---|---|---|---|
| **Structure** | One application, no internal boundaries | One application, split into modules with clear boundaries | Several independent services |
| **Development and deployment** | Simple | Simple: one codebase, one deployment | Complex: many deployments, network communication |
| **Fit for a small team** | Good at first | Good | Poor: high operational effort |
| **Code organisation over time** | Boundaries erode, code gets harder to change | Boundaries keep the code organised | Strong boundaries, enforced by the network |
| **Scaling** | Whole application | Whole application | Per service |
| **Fit for the MVP** | Acceptable, but harder to evolve | **Best fit** | Too complex for the MVP |

## 3. Decision

Matriz is built as a **modular monolith** ([ADR 0001](decisions/0001-use-modular-monolith.md)).

**Why:**
- It is as simple to build, test and deploy as a monolith, which suits a small team and a short timeline.
- Modules with clear boundaries keep accounts, projects and tasks separated, so the code stays easy to change as later features (notifications, progress summary) are added.
- Microservices solve problems the MVP doesn't have, such as independent scaling, at a high operational cost.
- A module can still be extracted into a separate service later, if needed.

## 4. Components

```
        Client
          |
          | REST / JSON over HTTP
          v
+-------------------------------------+
|  Matriz application                 |
|                                     |
|  REST API                           |
|     |          |          |         |
|  Accounts   Projects    Tasks       |
+-------------------------------------+
          |
          v
      PostgreSQL
```

| Component | Responsibilities | Requirements |
|---|---|---|
| **Client** | Shows the screens (sign in, dashboard, projects, project detail, task detail, "My tasks", delete account) and sends requests to the API. Contains no business logic. | NFR-01 |
| **REST API** | Receives requests, checks that the user is signed in (session cookie, [ADR 0004](decisions/0004-use-email-password-with-server-side-sessions.md)), validates the input, calls the right module and returns HTTP responses and errors. Contains no business logic. The contract is in [openapi.yaml](openapi.yaml). | NFR-02, [ADR 0003](decisions/0003-use-rest-between-clients-and-api.md) |
| **Accounts module** | Sign in, sign out and the signed-in user. Stores passwords securely, limits repeated failed sign-ins and expires inactive sessions. Deletes accounts and personal data. | FR-01, FR-14, NFR-02 |
| **Projects module** | Creates, lists, searches, edits and deletes projects. Manages members and the project manager role. Decides whether a user can see or change a project. | FR-02, FR-03, FR-16, NFR-02 |
| **Tasks module** | Creates, edits, filters and deletes tasks, and changes their status. Manages assignees and comments. Provides the project board, "My tasks" and the task counts per status. Decides when a task is late. | FR-04 to FR-10, FR-15, FR-17 |
| **PostgreSQL** | Stores users, projects, members, tasks, assignments, comments, failed sign-in attempts and sessions. Each module owns its own tables. | NFR-03, [ADR 0002](decisions/0002-use-postgresql.md) |

### Rules between modules

- A module only uses another module through its public interface, never its tables.
- **Tasks → Projects:** checks that the user is a project member, and that an assignee is a member of the project (US-04, US-10).
- **Projects → Tasks:** removing a member removes their assignments in that project (US-03). Deleting a project deletes its tasks, assignments and comments (US-02). The dashboard gets each project's task counts from Tasks (US-14).
- **Accounts → Projects:** an account can't be deleted while the user is the project manager of a project (EC-06).
- **Accounts → Projects and Tasks:** when an account is deleted, the user's name and email are no longer shown, their memberships and assignments are removed, and their comments are kept without an author (US-09).

## 5. Technologies and hosting

- Spring Boot (backend) and Angular (client) ([ADR 0005](decisions/0005-use-spring-boot-and-angular.md)).
- Runs locally, without HTTPS ([ADR 0006](decisions/0006-run-locally.md)). The session cookie is `HttpOnly` and `SameSite=Lax`, but not `Secure`, because there is no HTTPS.

### Implementation choices

| Area | Choice |
|---|---|
| Repository layout | `backend/` (Spring Boot) and `frontend/` (Angular) in this repository. |
| Backend | Java 21, Spring Boot 4.1, Maven (with the Maven wrapper). Packages `accounts`, `projects` and `tasks`, one per module. |
| Data access | Spring Data JPA. |
| Schema | Liquibase changelogs in formatted SQL (`backend/src/main/resources/db/changelog/`), including the Spring Session JDBC tables. |
| Demo data | A Liquibase changeset with the `demo` context. It runs once, so restarting doesn't delete or duplicate data. Demo passwords are stored as hashes; the fake demo password is in the README. |
| Failed sign-ins | Counted in a `sign_in_attempt` table by the Accounts module. Spring Security doesn't provide this. |
| Database | PostgreSQL 18 in a local Docker container (`docker-compose.yml`), with a named volume so data is kept. Connection set with the `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` variables, read from `.env`; `.env.example` has fake values. |
| Health check | Spring Boot Actuator, only the `health` endpoint (`/api/actuator/health`), to check that the backend reaches the database. |
| Frontend | Angular 21 (current LTS). The API address is set in one place, and the dev server proxies `/api` to the backend so the session cookie stays on the same origin. |
| Tests | JUnit 5, Spring Boot Test and MockMvc, with Testcontainers for PostgreSQL (needs Docker). Angular's default unit test runner (Vitest). End-to-end: Gherkin scenarios run by Playwright (Chromium) with playwright-bdd, with a separate in-memory PostgreSQL (`db-e2e`, Docker Compose profile `e2e`). |
| Static analysis and dependency checks | ESLint with angular-eslint (frontend); SpotBugs with FindSecBugs (backend, run on demand); `npm audit`, and OSV-Scanner run with Docker for the backend dependencies. |
