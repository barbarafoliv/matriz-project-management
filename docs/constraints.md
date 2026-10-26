# Matriz: Constraints and Assumptions

> Complements [requirements.md](requirements.md) and [user-stories.md](user-stories.md).

## 1. Known constraints

- The MVP includes only the *Must* requirements (FR-01 to FR-08, FR-14 and NFR-01 to NFR-03).
- The team is small and the timeline is short.
- The application must comply with the GDPR.
- The interface and documentation are in English.

## 2. Assumptions

- Small teams that track work in spreadsheets and chat lose track of who is doing what, and of what's late.
- Matriz is used by small teams, in one shared workspace.
- There are two user types: project manager and team member. These are roles in each project, not types of account: a user can be the project manager of one project and a team member of another.
- The user who creates a project is its project manager. Each project has one project manager, who can't be removed or replaced.
- Each task has at most one assignee.
- The statuses To Do, In Progress and Done are enough.
- Users work mainly on a computer.

## 3. Security and privacy

- Only signed-in users can use the application.
- Users only see the projects they are members of.
- Passwords are never stored in readable form.
- Only the name and email of each user are collected.
- Users can delete their account and personal data.
- Repeated failed sign-in attempts are limited, and inactive sessions expire.

## 4. Pending technical decisions

Decided so far:

- Type of application: a modular monolith ([ADR 0001](decisions/0001-use-modular-monolith.md)), with a REST API between clients and the application ([ADR 0003](decisions/0003-use-rest-between-clients-and-api.md)).
- Database: PostgreSQL ([ADR 0002](decisions/0002-use-postgresql.md)).
- Sign-in: email and password, with server-side sessions ([ADR 0004](decisions/0004-use-email-password-with-server-side-sessions.md)).

Still pending:

- The technologies to use for the client and the application.
- Where the application is hosted.

## 5. Scope limitations

- Only the MVP is in scope for the first version.
- Comments, filters, progress summary, notifications and the priority matrix view come later.
- Other features out of scope are listed in [requirements.md, section 6](requirements.md#6-out-of-the-initial-scope).
