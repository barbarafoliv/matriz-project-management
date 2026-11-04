---
title: Implementation Plan
---

# Matriz: Implementation Plan

> Builds the MVP in [requirements.md](requirements.md) with the stack and choices in [architecture.md](architecture.md). Each increment is analysed and planned, approved, implemented, checked, reviewed and committed before the next one starts.

## 1. Status

| # | Increment | Stories | Status |
|---|---|---|---|
| 0 | Reconcile the documentation | — | Done |
| 1 | Project skeleton and persistence | — | Done |
| 2 | Authentication | US-01 | To do |
| 3 | Projects, search and members | US-02, US-12, US-03 | To do |
| 4 | Tasks, status, board, filters and late tasks | US-04, US-05, US-07, US-13, US-08 | To do |
| 5 | Assignees and "My tasks" | US-10, US-06 | To do |
| 6 | Comments | US-11 | To do |
| 7 | Dashboard | US-14 | To do |
| 8 | Account deletion | US-09 | To do |
| 9 | Verification and delivery | All | To do |

## 2. Increments

### 0. Reconcile the documentation

- **Components:** requirements, user stories, constraints, architecture, data model, OpenAPI, UI decisions, ADR 0005, AGENTS.md, README.
- **Acceptance criteria:** every item of the functional scope maps to an FR and a US, and every FR maps to an endpoint and a table.
- **Checks:** traceability review; all `$ref`s in the OpenAPI resolve.

### 1. Project skeleton and persistence

- **Components:**
  - `docker-compose.yml`: PostgreSQL 18 with a named volume.
  - `backend/`: Spring Boot 4.1 application (Maven wrapper, Java 21), packages `accounts`, `projects`, `tasks`; `application.yml` reading `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` from `.env`; Actuator health endpoint.
  - Liquibase changelogs (formatted SQL): tables `app_user`, `project`, `project_member`, `task`, `task_assignee`, `comment`, `sign_in_attempt`, and the Spring Session tables.
  - Demo seed (Liquibase, `demo` context): Alice, Bob and Carol (fake data). "Website redesign" (PM Alice, member Bob), "Mobile launch" (PM Bob, member Alice), "Research notes" (PM Carol, no other members), with tasks in every status and priority, one late task, assignments and comments.
  - `frontend/`: Angular application with the `/api` dev proxy and the API address set in one place.
  - `.env.example` with fake values; README run commands.
- **Depends on:** Increment 0. JDK 21, Node.js and Docker installed.
- **Acceptance criteria:** backend and frontend start; the schema and seed are created on first start; restarting doesn't delete or duplicate data.
- **Checks:** `./mvnw verify` with a `@SpringBootTest` that checks the schema and seed on PostgreSQL; `ng build`; starting the backend twice and comparing row counts.
- **Result:** `./mvnw verify` 9 tests passed (schema, seed counts, second Liquibase run, duplicate assignment, email case, second PM, blank title, health). `ng build` passed, `ng test` 2 tests passed. Manual: first start ran 9 changesets; restart ran 0, and row counts were unchanged (3 users, 3 projects, 5 members, 8 tasks, 9 assignments, 4 comments). `/api/actuator/health` returned `db: UP`, directly and through the `ng serve` proxy.

### 2. Authentication (US-01, NFR-02)

- **Components:**
  - Backend: Spring Security configuration (session cookie, CSRF, 30-minute timeout), sign-in, sign-out and `GET /users/me`, BCrypt password hashing, failed sign-in counter (`sign_in_attempt`), `problem+json` error handler.
  - Frontend: sign-in screen, auth service, HTTP interceptor (401 → sign-in with "Your session has expired" message), route guard, top nav with the user's name and sign-out.
- **Depends on:** Increment 1.
- **Acceptance criteria:** US-01, EC-02. A request without a session gets 401. The 6th failed attempt gets 429 with `Retry-After`. After sign-out, `GET /users/me` gets 401.
- **Checks:** MockMvc tests for valid and invalid credentials, no session, lockout and sign-out; manual sign-in with two demo accounts.

### 3. Projects, search and members (US-02, US-12, US-03)

- **Components:**
  - Backend: project entity, repository, service and controller (CRUD, `?q=` search); members service and controller; access checks (non-member → 404, not the PM → 403). The PM is taken from the session.
  - Frontend: Projects screen (list, search box, empty and error states), create and edit modals, delete confirmation, Manage Members modal, project detail page (data only for now).
- **Depends on:** Increment 2.
- **Acceptance criteria:** US-02, US-12, US-03, EC-03 (name), EC-04, EC-07, EC-11. **First complete path:** sign in, create a project, see it in the list, and see it again after reloading the page and restarting the backend. Deleting a project leaves no orphan rows.
- **Checks:** **authorisation test**: user B gets 404 on user A's project, even with the project ID changed in the request. **Persistence test**: create a project, then read it back from the database. Manual run of the first complete path.

### 4. Tasks, status, board, filters and late tasks (US-04, US-05, US-07, US-13, US-08)

- **Components:**
  - Backend: task entity, repository, service and controller (CRUD, `PUT .../status`, `?status=&priority=` filters), late flag, validation (title 3–150, not blank; description ≤2000; priority MEDIUM by default).
  - Frontend: board on the project detail page, filter bar, new task form, task detail page (data, edit, status buttons, delete for the PM), late badge.
- **Depends on:** Increment 3.
- **Acceptance criteria:** US-04, US-05, US-07, US-13, US-08, EC-03 (title), EC-05, EC-11. The board and filters update without reloading the page.
- **Checks:** **validation test**: a title made only of spaces gets 400 `BLANK`, and nothing is saved. Unit test for the late rule (yesterday, today, no date, done). MockMvc tests for the filters. Manual board check.

### 5. Assignees and "My tasks" (US-10, US-06)

- **Components:**
  - Backend: assignee service and controller (`POST .../assignees`, `DELETE .../assignees/{userId}`), eligibility check through the Projects module, removal of assignments when a member is removed, `GET /users/me/tasks`.
  - Frontend: assignee list and `AssigneePicker` on the task detail page, assignee initials on task cards, My Tasks screen.
- **Depends on:** Increment 4.
- **Acceptance criteria:** US-10, US-06, EC-08, EC-09. Assignments are still there after reopening the task.
- **Checks:** MockMvc tests for duplicate (409), non-member (400) and removal; repository test showing the primary key rejects a duplicate; manual check with two assignees.

### 6. Comments (US-11)

- **Components:**
  - Backend: comment entity, service and controller (`GET` and `POST .../comments`). Author and date are set by the server.
  - Frontend: `CommentList` and `CommentForm` on the task detail page.
- **Depends on:** Increment 4.
- **Acceptance criteria:** US-11, EC-10. Comments are still there after reopening the task.
- **Checks:** MockMvc test that sends a different author in the request and checks the saved author is the signed-in user; validation tests (blank, 2001 characters); non-member gets 404.

### 7. Dashboard (US-14)

- **Components:**
  - Backend: task counts per status for the caller's projects, counted in one database query, returned as `taskCounts` with each project.
  - Frontend: dashboard (home screen) with the user's name, project cards with counts and links to each project.
- **Depends on:** Increments 3 and 4.
- **Acceptance criteria:** US-14. The counts match all of each project's tasks, not just the filtered or visible ones, and are updated after creating, editing or deleting a task.
- **Checks:** repository test for the counts against the seed; manual check after changing a task's status.

### 8. Account deletion (US-09, FR-14)

- **Components:**
  - Backend: account deletion in the Accounts module, calling Projects and Tasks to remove memberships and assignments and clear comment authors; session ended.
  - Frontend: Delete Account modal, including the EC-06 message.
- **Depends on:** Increments 5 and 6.
- **Acceptance criteria:** US-09, EC-06.
- **Checks:** service test covering all three modules; manual check that comments show "Deleted user".

### 9. Verification and delivery

- **Components:** `docs/verification.md` (cases 01 to 09: expected result, observed result, evidence; manual checks kept apart from automated tests; three examples of using AI). README (requirements, installation, configuration, schema, seed, demo accounts, starting the app, running the tests). AGENTS.md commands. OpenAPI updated to match the code. One refactoring, in its own commit.
- **Depends on:** Increments 1 to 8.
- **Acceptance criteria:** the application starts by following the README from a clean clone, and every verification case has a recorded result.
- **Checks:** full test suite (`./mvnw verify`, `ng test`), manual cases 01 to 09, run from a clean clone, final version tagged in Git.
