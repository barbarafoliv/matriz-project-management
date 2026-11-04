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
| [AGENTS.md](AGENTS.md) | Instructions for coding agents working on the project. |

The documents are traceable: each user story (US-xx) is linked to a functional requirement (FR-xx) in the requirements.

## Current status

**Phase: MVP implementation.** Project skeleton, database schema, demo data and sign-in are in place (Increments 1 and 2). Progress is tracked in the [implementation plan](docs/implementation-plan.md).

- [x] Problem analysis and clarification
- [x] Requirements and MVP scope
- [x] MVP user stories and acceptance criteria
- [x] Technical design and technology choice (architecture, data model, API contract and ADRs 0001 to 0006)
- [x] Implementation plan
- [ ] MVP implementation
- [ ] Testing against the acceptance criteria
