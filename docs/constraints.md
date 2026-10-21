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
- There are two user types: project manager and team member.
- The user who creates a project is its project manager.
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

- Type of application and the technologies to use.
- Database for storing users, projects and tasks.
- How users sign in.
- Where the application is hosted.

## 5. Scope limitations

- Only the MVP is in scope for the first version.
- Comments, filters, progress summary, notifications and the priority matrix view come later.
- Other features out of scope are listed in [requirements.md, section 6](requirements.md#6-out-of-the-initial-scope).
