# Matriz: Constraints and Assumptions

> Complements [requirements.md](requirements.md) and [user-stories.md](user-stories.md).

## 1. Known constraints

- The MVP includes only the *Must* requirements (FR-01 to FR-10, FR-14 to FR-17 and NFR-01 to NFR-03).
- The team is small and the timeline is short.
- The application must comply with the GDPR.
- The interface and documentation are in English.

## 2. Assumptions

- Small teams that track work in spreadsheets and chat lose track of who is doing what, and of what's late.
- Matriz is used by small teams, in one shared workspace.
- The MVP runs on one machine ([ADR 0006](decisions/0006-run-locally.md)). Team features are tried with several accounts on that machine.
- Accounts for the MVP are demo accounts created by a seed. Public sign-up (FR-18) and password recovery (FR-19) are optional extensions.
- There are two user types: project manager and team member. These are roles in each project, not types of account: a user can be the project manager of one project and a team member of another.
- The user who creates a project is its project manager. Each project has one project manager, who can't be removed or replaced.
- A task can have several assignees. Each assignee is a member of the task's project, and is assigned to it at most once.
- Any project member can assign and unassign members, and comment on tasks. Comments can't be edited or deleted in the MVP.
- The statuses To Do, In Progress and Done are enough.
- Users work mainly on a computer.

## 3. Security and privacy

- Only signed-in users can use the application.
- Users only see the projects they are members of, and their tasks, assignments and comments. This is checked by the backend on every request.
- The project manager and the author of a comment are taken from the session, never from the request.
- Passwords are never stored in readable form, and must be 8 to 64 characters long.
- Only the name and email of each user are collected.
- Users can delete their account and personal data.
- After 5 failed sign-in attempts, sign-in is blocked for 15 minutes. Sessions expire after 30 minutes of inactivity.
- Demo accounts use fake names, emails and a fake password. The fake demo password is the only password that may appear in the documentation (README).

## 4. Pending technical decisions

Decided so far:

- Type of application: a modular monolith ([ADR 0001](decisions/0001-use-modular-monolith.md)), with a REST API between clients and the application ([ADR 0003](decisions/0003-use-rest-between-clients-and-api.md)).
- Database: PostgreSQL ([ADR 0002](decisions/0002-use-postgresql.md)).
- Sign-in: email and password, with server-side sessions ([ADR 0004](decisions/0004-use-email-password-with-server-side-sessions.md)).
- Application stack: Spring Boot (backend) and Angular (frontend) ([ADR 0005](decisions/0005-use-spring-boot-and-angular.md)).
- Hosting: local only ([ADR 0006](decisions/0006-run-locally.md)).
- API contract: [openapi.yaml](openapi.yaml).
- Implementation choices (build tool, data access, migrations, seed, tests): [architecture.md, section 5](architecture.md#5-technologies-and-hosting).

All technical decisions for the MVP have been made.

## 5. Scope limitations

- Only the MVP is in scope for the first version.
- Progress summary, notifications, the priority matrix view, filtering by assignee, public sign-up and password recovery come later.
- Other features out of scope are listed in [requirements.md, section 6](requirements.md#6-out-of-the-initial-scope).
