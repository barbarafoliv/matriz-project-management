# Matriz

[![CI](https://github.com/barbarafoliv/matriz-project-management/actions/workflows/ci.yml/badge.svg)](https://github.com/barbarafoliv/matriz-project-management/actions/workflows/ci.yml)

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

## Try it online

**https://matriz-c1ft.onrender.com**: sign in with one of the [demo accounts](#demo-accounts).

It runs on free plans (Render and Neon), so after about 15 minutes without visitors the backend sleeps, and the next request takes about a minute. The demo data is shared by everyone who signs in, so it may have been changed.

## Getting started

### Requirements

- Docker with Docker Compose (for example Rancher Desktop or Docker Desktop), running. This is enough to run the application.
- For development and the tests: JDK 21, and Node.js 24 with npm 11.6 (`packageManager` in `frontend/package.json`; newer npm versions reject the lock file in `npm ci`).

### Set up

From the repository root:

```sh
cp .env.example .env      # then change both passwords in .env to the same new value (local, never committed)
```

### Run with Docker Compose

```sh
docker compose up -d      # builds the images and starts db, backend and frontend
docker compose ps         # all three should be "healthy"
```

The application is on **http://localhost:4200**. The frontend (nginx) forwards `/api` to the backend, which isn't published on its own port. The data is kept in the `matriz-db` volume. `docker compose logs --tail=50` shows the latest logs. Compose only builds the images when they don't exist yet, so after a code change, or a `git pull`, use `docker compose up -d --build`.

### Run for development

```sh
docker compose up -d db                  # only PostgreSQL 18, on localhost:5432
cd backend && ./mvnw spring-boot:run     # API on http://localhost:8080/api
cd frontend && npm install && npm start  # app on http://localhost:4200, /api forwarded to the backend
```

`npm start` and the Docker Compose frontend both use port 4200, so stop one before starting the other (`docker compose stop frontend backend`).

On first start, the backend creates the schema and loads the demo data with Liquibase. Later starts skip what has already run, so data isn't deleted or duplicated.

To check that the backend reaches the database, `curl http://localhost:4200/api/actuator/health` (or `:8080` in development) should show `"db":{"status":"UP"}`.

### Database schema and demo data

The schema is created by Liquibase when the backend starts (`backend/src/main/resources/db/changelog/`). The demo data is a changeset with the `demo` context, which runs once: restarting doesn't delete or duplicate data. To start again from an empty database, see [Stop](#stop).

### Demo accounts

Created by the seed, locally and online. All use the fake password **`matriz-demo-2026`**.

| Name | Email | Projects |
|---|---|---|
| Alice Example | alice@example.com | Website redesign (project manager), Mobile launch |
| Bob Example | bob@example.com | Mobile launch (project manager), Website redesign |
| Carol Example | carol@example.com | Research notes (project manager) |

### Sessions

Signing in creates a session on the server (stored in PostgreSQL). The browser keeps its ID in the `SESSION` cookie, which scripts can't read. Online, the session and CSRF cookies are only sent over HTTPS (`COOKIES_SECURE=true`); locally they aren't, because there is no HTTPS. The session ends on sign-out, or after 30 minutes without requests; the app then asks you to sign in again. After 5 wrong passwords in a row for an account, sign-in to it is blocked for 15 minutes. Details are in [docs/openapi.yaml](docs/openapi.yaml).

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

Secrets in the code and its history are checked with [gitleaks](https://github.com/gitleaks/gitleaks), also run with Docker, from the repository root:

```sh
docker run --rm -v "$(pwd):/repo:ro" zricethezav/gitleaks:latest git /repo --redact
```

### Continuous integration

Every push and pull request runs [`.github/workflows/ci.yml`](.github/workflows/ci.yml) on GitHub Actions, with read-only permissions and no secrets:
- backend tests and SpotBugs;
- frontend lint, tests and build;
- the end-to-end test.

The Docker images are built only after all three pass. Results are in the repository's **Actions** tab. When a test fails, the test reports are attached to the run.

### Stop

```sh
docker compose stop       # stops the containers, keeps the data
docker compose down       # also removes the containers; the data is still kept
```

`docker compose down -v` also **deletes the database volume** and all data; the next backend start recreates the schema and demo data.

## Deployment

The online version runs on Render, with the database on Neon ([ADR 0008](docs/decisions/0008-host-on-render-and-neon.md), [architecture, section 6](docs/architecture.md#6-deployment)). [`render.yaml`](render.yaml) describes both services:
- a static site for the frontend, which forwards `/api` to the backend;
- a web service built from `backend/Dockerfile`.

Render deploys `main` after the CI checks pass.

The database settings (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) are only set in the Render dashboard. `DB_URL` uses Neon's *direct* (not pooled) connection, as `jdbc:postgresql://<host>/<database>?sslmode=require&channelBinding=require`.

## Documentation

| Document | Contents |
|---|---|
| [docs/requirements.md](docs/requirements.md) | Vision and goals, user types, functional and non-functional requirements, MVP scope, and features out of the initial scope. |
| [docs/user-stories.md](docs/user-stories.md) | The MVP backlog: 14 user stories (one per MVP requirement) with priorities, Given / When / Then acceptance criteria, and exceptional cases. |
| [docs/constraints.md](docs/constraints.md) | Known constraints, assumptions, security and privacy requirements, pending technical decisions, and scope limitations. |
| [docs/architecture.md](docs/architecture.md) | Architecture alternatives, the chosen modular monolith, its main components and responsibilities, and how it is deployed. |
| [docs/database.md](docs/database.md) | Conceptual data model: entities, relationships, integrity rules and traceability to the requirements. |
| [docs/decisions/](docs/decisions/) | Architecture Decision Records (ADRs). |
| [docs/ui/](docs/ui/design-decisions.md) | UI prototype ([prototype.html](docs/ui/prototype.html)) and UI/UX design decisions. |
| [docs/openapi.yaml](docs/openapi.yaml) | REST API contract (OpenAPI 3.1): endpoints, DTOs, validation rules and errors. |
| [docs/implementation-plan.md](docs/implementation-plan.md) | Implementation increments, with components, dependencies, acceptance criteria, checks and status. |
| [docs/verification.md](docs/verification.md) | Verification results: commands, cases 01–09 with evidence, bugs fixed, limitations and use of AI. |
| [docs/test-plan.md](docs/test-plan.md) | Test plan for tasks and their permissions: scenarios, expected results, test levels and the tests that cover them. |
| [docs/quality-security-review.md](docs/quality-security-review.md) | Code Review and Security Review: findings with evidence, access checks between accounts, static analysis, dependency scans and decisions. |
| [CHANGELOG.md](CHANGELOG.md) | Changes in each version. |
| [docs/releases/](docs/releases/1.1.0-rc.1.md) | Release notes, starting with the 1.1.0-rc.1 release candidate. |
| [AGENTS.md](AGENTS.md) | Instructions for coding agents working on the project. |

The documents are traceable: each user story (US-xx) is linked to a functional requirement (FR-xx) in the requirements.

## Current status

**Phase: release candidate 1.1.0-rc.1.** All *Must* requirements are built and verified:
- sign-in and the dashboard;
- projects, search and members;
- tasks, with the board, filters and late tasks;
- assignees and "My tasks";
- comments and account deletion.

The application also runs with Docker Compose, is checked by CI, and is online. The results are in [docs/verification.md](docs/verification.md), and the increments in the [implementation plan](docs/implementation-plan.md). The changes are in the [changelog](CHANGELOG.md).

- [x] Problem analysis and clarification
- [x] Requirements and MVP scope
- [x] MVP user stories and acceptance criteria
- [x] Technical design and technology choice (architecture, data model, API contract and ADRs 0001 to 0006)
- [x] Implementation plan
- [x] MVP implementation
- [x] Testing against the acceptance criteria
- [x] Docker images, Docker Compose and CI
- [x] Online deployment (Render and Neon)
- [ ] Release 1.1.0 (after the release candidate is validated)
