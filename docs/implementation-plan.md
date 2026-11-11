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
| 2 | Authentication | US-01 | Done |
| 3 | Projects, search and members | US-02, US-12, US-03 | Done |
| 4 | Tasks, status, board, filters and late tasks | US-04, US-05, US-07, US-13, US-08 | Done |
| 5 | Assignees and "My tasks" | US-10, US-06 | Done |
| 6 | Comments | US-11 | Done |
| 7 | Dashboard | US-14 | Done |
| 8 | Account deletion | US-09 | Done (built before 7) |
| 9 | Verification and delivery | All | Done |
| 10 | Docker images | — | Done |
| 11 | Docker Compose | — | Planned |
| 12 | CI pipeline | — | Planned |
| 13 | Documentation and release candidate 1.1.0-rc.1 | — | Planned |
| 14 | Final validation | All | Planned |

Increments 10 to 14 are Exercises 2 to 6 of Practical Worksheet 05. They prepare the delivery with Docker, Docker Compose and GitHub Actions ([ADR 0007](decisions/0007-run-with-docker-compose-and-check-with-github-actions.md)). They don't change any feature.

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
- **Decisions:** failed sign-ins are only counted for existing accounts. The home screen is a placeholder ("Hello, *name*") until Increment 7.
- **Result:** `./mvnw verify` 26 tests passed (16 sign-in tests with MockMvc, 1 cookie test on a real server, plus Increment 1's). `ng test` 17 tests passed. Manual, in the browser: a protected page redirects to sign-in; a wrong password shows "Email or password is incorrect." and keeps the email; Alice and Bob sign in and see their own names, also after a reload; `SESSION` is `HttpOnly` and not visible to scripts; deleting the session in the database sends the user back to sign-in; sign-out returns to sign-in. Found and fixed during the check: the `XSRF-TOKEN` cookie had `Path=/api`, so the browser couldn't send the token (now `Path=/`, with a regression test), and anonymous requests created sessions (request cache disabled, with a test). The "session expired" message is covered by unit tests; it can be seen in the browser from Increment 3, when pages call the API after loading.

### 3. Projects, search and members (US-02, US-12, US-03)

- **Components:**
  - Backend: project entity, repository, service and controller (CRUD, `?q=` search); members service and controller; access checks (non-member → 404, not the PM → 403). The PM is taken from the session.
  - Frontend: Projects screen (list, search box, empty and error states), create and edit modals, delete confirmation, Manage Members modal, project detail page (data only for now).
- **Depends on:** Increment 2.
- **Acceptance criteria:** US-02, US-12, US-03, EC-03 (name), EC-04, EC-07, EC-11. **First complete path:** sign in, create a project, see it in the list, and see it again after reloading the page and restarting the backend. Deleting a project leaves no orphan rows.
- **Checks:** **authorisation test**: user B gets 404 on user A's project, even with the project ID changed in the request. **Persistence test**: create a project, then read it back from the database. Manual run of the first complete path.
- **Result:** `./mvnw verify` 47 tests passed (12 project tests and 9 member tests added; both sheet tests are in `ProjectTests`). `ng test` 35 tests passed. Manual, in the browser: Alice only sees her 2 projects; a blank name is rejected; the new project appears in the list without a reload, and is still there after reloading the page and restarting the backend; search ignores case and shows the no-results message; edit; Manage Members rejects an unknown email and adds Bob; Bob sees the project without PM actions; Carol gets "Project not found." for its URL; removing a member and deleting the project ask for confirmation; no rows are left after the delete; "Your session has expired" shows after the session is deleted in the database. Found and fixed during the check: signing in again right after signing out failed with 403, because sign-out removes the CSRF token and the app didn't fetch a new one (regression test in `auth.service.spec.ts`).

### 4. Tasks, status, board, filters and late tasks (US-04, US-05, US-07, US-13, US-08)

- **Components:**
  - Backend: task entity, repository, service and controller (CRUD, `PUT .../status`, `?status=&priority=` filters), late flag, validation (title 3–150, not blank; description ≤2000; priority MEDIUM by default).
  - Frontend: board on the project detail page, filter bar, new task form, task detail page (data, edit, status buttons, delete for the PM), late badge.
- **Depends on:** Increment 3.
- **Acceptance criteria:** US-04, US-05, US-07, US-13, US-08, EC-03 (title), EC-05, EC-11. The board and filters update without reloading the page.
- **Checks:** **validation test**: a title made only of spaces gets 400 `BLANK`, and nothing is saved. Unit test for the late rule (yesterday, today, no date, done). MockMvc tests for the filters. Manual board check.
- **Result:** `./mvnw verify` 61 tests passed (14 task tests added; the sheet's validation test is `TaskTests.rejectsATitleMadeOnlyOfSpacesAndSavesNothing`). `ng test` 51 tests passed (first reported as 57: a test helper imported from a spec file made one suite run twice; fixed in Increment 5). Manual, in the browser: the board shows the demo tasks in their columns and the late one with a border and a "Late" label; the priority and status filters ask the API and show "No tasks match these filters." with Clear filters; a blank title is rejected, the default priority is Medium, and a new task appears without a reload; status buttons save straight away and the project counts follow; editing saves; marking the late task Done removes "Late"; the task is still there after a backend restart; Bob edits but can't delete, Carol gets "Task not found.", Alice deletes after confirming; at 390 px wide the columns stack and nothing overflows. The task page shows assignees read-only; assigning comes in Increment 5. There is no drag-and-drop: status changes on the task page (docs/ui/design-decisions.md).

### 5. Assignees and "My tasks" (US-10, US-06)

- **Components:**
  - Backend: assignee service and controller (`POST .../assignees`, `DELETE .../assignees/{userId}`), eligibility check through the Projects module, removal of assignments when a member is removed, `GET /users/me/tasks`.
  - Frontend: assignee list and `AssigneePicker` on the task detail page, assignee initials on task cards, My Tasks screen.
- **Depends on:** Increment 4.
- **Acceptance criteria:** US-10, US-06, EC-08, EC-09. Assignments are still there after reopening the task.
- **Checks:** MockMvc tests for duplicate (409), non-member (400) and removal; repository test showing the primary key rejects a duplicate; manual check with two assignees.
- **Result:** `./mvnw verify` 69 tests passed (8 assignee and "My tasks" tests added; the duplicate-key test is in `SchemaAndSeedTests`). `ng test` 61 tests passed. Manual, in the browser: the picker only offers project members who aren't assigned; Alice and Bob are assigned to the same task, still there after a reload; removing one works; with everyone assigned the panel says so; "My tasks" lists Alice's tasks from both projects by due date, with the undated one last and late ones labelled; a task opens its board with the card highlighted; Bob's list shows his own tasks. Found and fixed during the check: the picker started blank and Assign silently picked the first member; it now starts on "Choose a member…" and Assign is disabled until someone is chosen. Demo data was restored after the check (9 assignments).

### 6. Comments (US-11)

- **Components:**
  - Backend: comment entity, service and controller (`GET` and `POST .../comments`). Author and date are set by the server.
  - Frontend: `CommentList` and `CommentForm` on the task detail page.
- **Depends on:** Increment 4.
- **Acceptance criteria:** US-11, EC-10. Comments are still there after reopening the task.
- **Checks:** MockMvc test that sends a different author in the request and checks the saved author is the signed-in user; validation tests (blank, 2001 characters); non-member gets 404.
- **Result:** `./mvnw verify` 75 tests passed (6 comment tests added, including `ignoresAnAuthorOrDateSentInTheRequest`). `ng test` 67 tests passed. Manual, in the browser: the demo comments show oldest first with author and local time; a blank comment is rejected; Alice's new comment appears without a reload with her name and the current time, and the box is cleared; after a backend restart Bob sees it and his reply appears with his name. Comments from deleted accounts show as "Deleted user" (covered by tests; account deletion comes in Increment 8). Two comments written during the check are still in the local database.

### 7. Dashboard (US-14)

- **Components:**
  - Backend: task counts per status for the caller's projects, counted in one database query, returned as `taskCounts` with each project.
  - Frontend: dashboard (home screen) with the user's name, project cards with counts and links to each project.
- **Depends on:** Increments 3 and 4.
- **Acceptance criteria:** US-14. The counts match all of each project's tasks, not just the filtered or visible ones, and are updated after creating, editing or deleting a task.
- **Checks:** repository test for the counts against the seed; manual check after changing a task's status.
- **Result:** no backend change was needed: the counts were added in Increment 3, in one query (`ProjectTasks.countByStatus`), and are tested in `ProjectTests.countsTasksPerStatusOverAllOfTheProjectsTasks` and `TaskTests.projectCountsFollowStatusChanges`; `./mvnw verify` 82 tests passed. `ng test` 77 tests passed (5 dashboard tests added). The project card is now shared by the dashboard and the Projects screen. Manual, in the browser: after sign-in the dashboard shows "Hello, Alice Example" and her two projects with the demo counts; after moving a task to In Progress, the dashboard showed the new counts; the task was moved back afterwards. Fixed during the check: link-styled buttons were underlined.

### 8. Account deletion (US-09, FR-14)

- **Components:**
  - Backend: account deletion in the Accounts module, calling Projects and Tasks to remove memberships and assignments and clear comment authors; session ended.
  - Frontend: Delete Account modal, including the EC-06 message.
- **Depends on:** Increments 5 and 6.
- **Acceptance criteria:** US-09, EC-06.
- **Checks:** service test covering all three modules; manual check that comments show "Deleted user".
- **Result:** `./mvnw verify` 82 tests passed (7 account deletion tests added). `ng test` 72 tests passed. Manual, in the browser: Alice (a project manager) gets the EC-06 message listing her project and no Delete button; a throwaway non-manager account (fake data, inserted for the check) was added to a project, assigned to a task and commented, then deleted its account: it was signed out with "Your account and personal data were deleted." and couldn't sign in again; Alice then saw its comment as "Deleted user", and it was no longer an assignee or member. The check task was deleted afterwards and the demo data is unchanged. All demo accounts manage a project, so none of them can be deleted (EC-06).

### 9. Verification and delivery

- **Components:** `docs/verification.md` (cases 01 to 09: expected result, observed result, evidence; manual checks kept apart from automated tests; three examples of using AI). README (requirements, installation, configuration, schema, seed, demo accounts, starting the app, running the tests). AGENTS.md commands. OpenAPI updated to match the code. One refactoring, in its own commit.
- **Depends on:** Increments 1 to 8.
- **Acceptance criteria:** the application starts by following the README from a clean clone, and every verification case has a recorded result.
- **Checks:** full test suite (`./mvnw verify`, `ng test`), manual cases 01 to 09, run from a clean clone, final version tagged in Git.
- **Result:** see [verification.md](verification.md). An independent AI review of permissions, validation and error handling found no high-severity defect; 4 issues were fixed with tests and the contract was aligned with the code; 4 that need simultaneous requests are listed as open. Refactoring: shared priority and status badges. Final: 85 backend and 82 frontend tests pass.

### 10. Docker images

- **Components:**
  - `backend/Dockerfile`: multi-stage. A JDK 21 stage builds the jar with the Maven wrapper (tests skipped, because CI runs them). A Java 21 JRE stage runs it as a non-root user and exposes port 8080.
  - `frontend/Dockerfile`: multi-stage. A Node.js 24 stage runs `npm ci` and `ng build`. An nginx stage (`nginx-unprivileged`, non-root, port 8080) serves the files.
  - `frontend/nginx/default.conf`: forwards `/api` to `backend:8080`, and sends unknown paths to `index.html` so Angular routes work after a reload.
  - `backend/.dockerignore` and `frontend/.dockerignore`: leave out `target/`, `node_modules/`, test output, IDE files and `.env`.
- **Depends on:** Increment 9. Docker running.
- **Acceptance criteria:** both images build. Each container starts with `docker run`, with the database settings passed as environment variables. No credentials are in the Dockerfiles or images.
- **Checks:** `docker build`, `docker images`, `docker run`, `docker ps`, `docker logs`. `docker history` and a search of the Dockerfiles to confirm no credentials.
- **Result:** both images build (`matriz-backend` 581 MB, `matriz-frontend` 92 MB). Run with `docker run` on a Docker network, with a throwaway PostgreSQL 18 container and the settings from `.env`:
  - all three containers started; the backend runs as `matriz` (uid 999) and nginx as `nginx` (uid 101);
  - Liquibase created the schema and the demo data;
  - through nginx on port 4200, `/api/actuator/health` returned `db: UP`, `/api/users/me` returned 401 without a session, and `/projects/1` loaded the app;
  - in the browser, Alice signed in, saw her dashboard, opened "Website redesign" and created a task, which appeared without a reload and was still there after reloading the page;
  - the backend logs had no `WARN` or `ERROR` lines, and the nginx logs had no errors;
  - no credentials in `docker history`, the image environment or the Dockerfiles.
- **Found and fixed:** `npm ci` failed in the image, because the npm bundled with `node:24-alpine` (11.19.1) reports `@emnapi/core` and `@emnapi/runtime` missing from `package-lock.json`. npm 11.6.2, the version in `package.json` (`packageManager`), accepts the same lock file on Linux. The image now installs npm 11.6.2 first; the lock file is unchanged. CI must use the same npm version (Increment 12).

### 11. Docker Compose

- **Components:**
  - `docker-compose.yml`:
    - `backend` (built from `backend/`; `DB_URL` points to `db`; waits until `db` is healthy; healthcheck on `/api/actuator/health`; port 8080 not published).
    - `frontend` (built from `frontend/`; waits until `backend` is healthy; healthcheck on nginx; published on port 4200, the only service the browser uses).
    - `db` and its `matriz-db` volume stay as they are; `db-e2e` stays behind the `e2e` profile. The services talk over Compose's default network, by service name.
  - `.env.example`: any new variables, with fake values.
- **Depends on:** Increment 10.
- **Acceptance criteria:**
  - `docker compose up -d` starts all three services, and they become healthy.
  - In the browser, through the frontend: sign in, open a project, create a task and change its status.
  - The data is still there after `docker compose down` and `up -d`.
  - The logs have no unresolved errors.
  - The development flow (`docker compose up -d db`, `./mvnw spring-boot:run`, `npm start`) and `npm run e2e` still work.
- **Checks:** `docker compose config -q`, `build`, `up -d`, `ps`, `logs --tail=50`; manual check in the browser; recreate the containers and check the data.

### 12. CI pipeline

- **Components:** `.github/workflows/ci.yml`, run on `push` and `pull_request`, with `permissions: contents: read`. Jobs:
  - `backend`: JDK 21, `./mvnw verify` (Testcontainers uses the runner's Docker), `./mvnw spotbugs:check`.
  - `frontend`: Node.js 24, `npm ci`, `npm run lint`, `npx ng test --watch=false`, `npx ng build`.
  - `e2e`: `.env` created from `.env.example` (fake values only), Chromium installed, `npm run e2e`. The Playwright report is uploaded when it fails.
  - `docker`: `needs` the three jobs above; builds both images (`docker compose build`) and doesn't push them.
- **Depends on:** Increment 11.
- **Acceptance criteria:** the workflow passes on GitHub; it fails when a required test fails; images are built only after the checks pass; no secrets are used or printed.
- **Checks:** a passing run in the Actions tab; a temporary commit that breaks a test makes the run fail and skips `docker`; reverting it makes the run pass again.

### 13. Documentation and release candidate 1.1.0-rc.1

- **Components:**
  - README: running with Docker Compose, the development flow with `up -d db`, CI and its badge, and links to the new documents.
  - `docs/architecture.md`: the component diagram in Mermaid, and a section on how the containers are deployed.
  - `CHANGELOG.md`: 1.0.0 and 1.1.0-rc.1 (Keep a Changelog, Semantic Versioning).
  - Release notes for 1.1.0-rc.1.
  - The OpenAPI contract is already in `docs/openapi.yaml`. The API doesn't change, so it stays at version 1.0.0.
  - `AGENTS.md`: the Docker commands.
- **Depends on:** Increment 12.
- **Acceptance criteria:** every feature, technology, command and setting in the documentation matches the code. Anything that couldn't be confirmed is listed for review. The candidate version is proposed but not published.
- **Checks:** follow every documented command; check the Mermaid diagram renders on GitHub.

### 14. Final validation

- **Components:** `docs/verification.md`: a section for Practical Worksheet 05 covering the requirements met, the problems found and how they were fixed, the CI results, use of AI, and limitations.
- **Depends on:** Increment 13.
- **Acceptance criteria:**
  - The application runs by following the README from a clean clone.
  - The containers are healthy, and sign-in, projects, tasks and comments work.
  - CI passes.
  - The logs have been reviewed.
  - No credentials are in the tracked files.
- **Checks:** clean-clone run; `docker compose ps` and `logs`; manual check of the main features; gitleaks; an AI review of the documentation, Docker configuration, pipeline and test results.
