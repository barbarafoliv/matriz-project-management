# Matriz

Matriz is a simple shared workspace where a small team plans projects, organises tasks and sees progress in one place.

## The problem

Small teams that track work in spreadsheets and chat lose track of **who is doing what**, and of **what's late**. Finding out how a project is going means asking someone.

## Main goals

1. Everyone can see the status of a project at any time.
2. Everyone knows which tasks are theirs and when they're due.
3. Late tasks are easy to spot.

## MVP features

The MVP is the smallest version where a team can **create a project, assign tasks, discuss them, move them through the statuses and see what's late**.

| ID | Feature |
|---|---|
| FR-01 | Sign in and sign out, and see who is signed in. Demo accounts are created by a seed. |
| FR-02 | Create, list, open, edit and delete projects (name and optional description). |
| FR-03 | Add and remove project members. |
| FR-04 | Create, open and edit tasks (title, description, due date, priority). Only the project manager can delete them. |
| FR-05 | Change a task's status: To Do, In Progress, Done. |
| FR-06 | "My tasks": a list of the tasks assigned to me. |
| FR-07 | A project board with one column per status. |
| FR-08 | Late tasks are highlighted. |
| FR-09 | Add and list comments on a task, with author and date. |
| FR-10 | Filter tasks by status and priority. |
| FR-14 | Delete my account and personal data. |
| FR-15 | Assign several project members to a task, and remove assignments. |
| FR-16 | Search my projects by name. |
| FR-17 | Dashboard with my projects and their task counts per status. |

**Users:** project managers, who create projects and manage members, and team members, who create, edit, assign and comment on tasks. Users only see the projects they belong to.

Features planned for later versions (progress summary, notifications, priority matrix view, filter by assignee, public sign-up, password recovery) and features out of scope are listed in the [requirements](docs/requirements.md).

## Getting started

### Requirements

- JDK 21
- Node.js 24 and npm
- Docker with Docker Compose (for example Rancher Desktop or Docker Desktop), running

### Set up

From the repository root:

```sh
cp .env.example .env      # then change the two passwords in .env (local values, never committed)
docker compose up -d      # starts PostgreSQL 18 in Docker, data kept in the "matriz-db" volume
```

### Run

```sh
cd backend && ./mvnw spring-boot:run     # API on http://localhost:8080/api
cd frontend && npm install && npm start  # app on http://localhost:4200, /api forwarded to the backend
```

On first start, the backend creates the schema and loads the demo data with Liquibase. Later starts skip what has already run, so data isn't deleted or duplicated.

To check that the backend reaches the database: `curl http://localhost:8080/api/actuator/health` should show `"db":{"status":"UP"}`.

### Database schema and demo data

The schema is created by Liquibase when the backend starts (`backend/src/main/resources/db/changelog/`). The demo data is a changeset with the `demo` context, which runs once: restarting doesn't delete or duplicate data. To start again from an empty database, see [Stop](#stop).

### Demo accounts

Created by the seed. All use the fake password **`matriz-demo-2026`**.

| Name | Email | Projects |
|---|---|---|
| Alice Example | alice@example.com | Website redesign (project manager), Mobile launch |
| Bob Example | bob@example.com | Mobile launch (project manager), Website redesign |
| Carol Example | carol@example.com | Research notes (project manager) |

### Sessions

Signing in creates a session on the server (stored in PostgreSQL). The browser keeps its ID in the `SESSION` cookie, which scripts can't read. The session ends on sign-out, or after 30 minutes without requests; the app then asks you to sign in again. After 5 wrong passwords in a row for an account, sign-in to it is blocked for 15 minutes. Details are in [docs/openapi.yaml](docs/openapi.yaml).

### Test

```sh
cd backend && ./mvnw verify                  # needs Docker: tests use a temporary PostgreSQL (Testcontainers)
cd frontend && npm test -- --watch=false
```

End-to-end tests are written in Gherkin (`frontend/e2e/features/*.feature`, steps in `frontend/e2e/steps/`) and run by Playwright (Chromium) with [playwright-bdd](https://github.com/vitalets/playwright-bdd), which turns each scenario into a Playwright test (`npx bddgen`, done by `npm run e2e`). They run in their own environment: a separate, empty PostgreSQL (`db-e2e`, port 5433, data kept in memory) with the demo data, and their own backend (port 8081) and frontend (port 4201). The development database and servers aren't used, and can keep running. Needs Docker and `.env`.

```sh
cd frontend && npx playwright install chromium   # once
E2E_PASSWORD=matriz-demo-2026 npm run e2e         # recreates db-e2e, starts the servers, runs the tests as Alice
docker compose --profile e2e rm -sf db-e2e        # optional: remove the E2E database
```

On failure, the trace and screenshot are in `frontend/test-results/`; `npx playwright show-report` opens the report.

Static analysis and dependency checks (results in [docs/quality-security-review.md](docs/quality-security-review.md)):

```sh
cd backend && ./mvnw compile spotbugs:check   # SpotBugs with FindSecBugs; add -Dspotbugs.threshold=Low for all findings
cd frontend && npm run lint && npm audit      # ESLint with angular-eslint; known vulnerabilities in npm packages
```

Backend dependencies are checked with [OSV-Scanner](https://google.github.io/osv-scanner/), run once with Docker on a list of all dependencies:

```sh
cd backend && ./mvnw org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeBom -DincludeTestScope=true -DoutputFormat=json
docker run --rm -v "$(pwd)/target/bom.json:/src/bom.cdx.json:ro" ghcr.io/google/osv-scanner:latest scan source -L /src/bom.cdx.json
```

Behind a proxy that inspects HTTPS, also mount its CA certificate and add `-e SSL_CERT_FILE=/certs/ca.pem -v <ca-file>:/certs/ca.pem:ro`, or OSV-Scanner can't reach its database and reports no issues.

### Stop

```sh
docker compose stop       # stops PostgreSQL, keeps the data
```

`docker compose down -v` also **deletes the database volume** and all data; the next backend start recreates the schema and demo data.

## Documentation

| Document | Contents |
|---|---|
| [docs/requirements.md](docs/requirements.md) | Vision and goals, user types, functional and non-functional requirements, MVP scope, and features out of the initial scope. |
| [docs/user-stories.md](docs/user-stories.md) | The MVP backlog: 14 user stories (one per MVP requirement) with priorities, Given / When / Then acceptance criteria, and exceptional cases. |
| [docs/constraints.md](docs/constraints.md) | Known constraints, assumptions, security and privacy requirements, pending technical decisions, and scope limitations. |
| [docs/architecture.md](docs/architecture.md) | Architecture alternatives, the chosen modular monolith, and its main components and responsibilities. |
| [docs/database.md](docs/database.md) | Conceptual data model: entities, relationships, integrity rules and traceability to the requirements. |
| [docs/decisions/](docs/decisions/) | Architecture Decision Records (ADRs). |
| [docs/ui/](docs/ui/design-decisions.md) | UI prototype ([prototype.html](docs/ui/prototype.html)) and UI/UX design decisions. |
| [docs/openapi.yaml](docs/openapi.yaml) | REST API contract (OpenAPI 3.1): endpoints, DTOs, validation rules and errors. |
| [docs/implementation-plan.md](docs/implementation-plan.md) | Implementation increments, with components, dependencies, acceptance criteria, checks and status. |
| [docs/verification.md](docs/verification.md) | Verification results: commands, cases 01–09 with evidence, bugs fixed, limitations and use of AI. |
| [docs/test-plan.md](docs/test-plan.md) | Test plan for tasks and their permissions: scenarios, expected results, test levels and the tests that cover them. |
| [docs/quality-security-review.md](docs/quality-security-review.md) | Code Review and Security Review: findings with evidence, access checks between accounts, static analysis, dependency scans and decisions. |
| [AGENTS.md](AGENTS.md) | Instructions for coding agents working on the project. |

The documents are traceable: each user story (US-xx) is linked to a functional requirement (FR-xx) in the requirements.

## Current status

**Phase: MVP complete.** All *Must* requirements are built and verified: sign-in, dashboard, projects and search, members, tasks with board, filters and late tasks, assignees, "My tasks", comments and account deletion. The results are in [docs/verification.md](docs/verification.md); the increments in the [implementation plan](docs/implementation-plan.md).

- [x] Problem analysis and clarification
- [x] Requirements and MVP scope
- [x] MVP user stories and acceptance criteria
- [x] Technical design and technology choice (architecture, data model, API contract and ADRs 0001 to 0006)
- [x] Implementation plan
- [x] MVP implementation
- [x] Testing against the acceptance criteria
