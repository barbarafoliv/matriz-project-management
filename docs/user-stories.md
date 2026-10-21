# Matriz: User Stories (MVP)

> Based on the MVP scope in [requirements.md](requirements.md): one user story per *Must* functional requirement (FR-01 to FR-08 and FR-14).

**Statuses:** To Do, In Progress, Done. A new task starts as **To Do**.
**Late task:** due date before today and status not **Done**.

## 1. User stories and priorities

All stories are part of the MVP (**Must**). The order is the order in which they should be built.

| Order | ID | User story | Requirement | Priority |
|---|---|---|---|---|
| 1 | US-01 | As a **user**, I want to sign up, sign in and sign out, so that my work is linked to my account. | FR-01 | Must |
| 2 | US-02 | As a **project manager**, I want to create, edit and delete projects, so that the team's work is organised in one place. | FR-02 | Must |
| 3 | US-03 | As a **project manager**, I want to add and remove project members, so that only the right people work on the project. | FR-03 | Must |
| 4 | US-04 | As a **project member**, I want to create and edit tasks, so that the work to be done is recorded. Only the **project manager** can delete them. | FR-04 | Must |
| 5 | US-05 | As a **project member**, I want to change a task's status, so that everyone knows how the work is progressing. | FR-05 | Must |
| 6 | US-06 | As a **team member**, I want to see the tasks assigned to me, so that I know what I have to do. | FR-06 | Must |
| 7 | US-07 | As a **project member**, I want to see the project's tasks on a board by status, so that I see the progress at a glance. | FR-07 | Must |
| 8 | US-08 | As a **project member**, I want late tasks to be highlighted, so that late work is easy to spot. | FR-08 | Must |
| 9 | US-09 | As a **user**, I want to delete my account, so that my personal data is removed. | FR-14 | Must |

## 2. Acceptance criteria

### US-01: Sign up, sign in and sign out
- **Given** I don't have an account, **when** I sign up with a name, email and password, **then** my account is created and I'm signed in.
- **Given** I have an account, **when** I sign in with the correct email and password, **then** I see my projects.
- **Given** I'm signed in, **when** I sign out, **then** I'm sent to the sign-in screen.
- **Given** I'm not signed in, **when** I try to open a project, **then** I'm sent to the sign-in screen.

### US-02: Create, edit and delete projects
- **Given** I'm signed in, **when** I create a project with a name, **then** it appears in my project list and I'm its project manager.
- **Given** I'm the project manager, **when** I change the project's name and save, **then** other members see the new name when they next open the project.
- **Given** I'm the project manager, **when** I delete the project and confirm, **then** the project and its tasks no longer appear in any member's project list or "My tasks".
- **Given** I'm a member but not the project manager, **when** I open the project, **then** I can't edit or delete it.

### US-03: Add and remove project members
- **Given** I'm the project manager, **when** I add a registered user by email, **then** the project appears in their project list.
- **Given** I'm the project manager, **when** I remove a member, **then** they can no longer see the project.

### US-04: Create, edit and delete tasks
- **Given** I'm a project member, **when** I create a task with a title, **then** it's added to the project with status **To Do**.
- **Given** I'm creating or editing a task, **when** I choose an assignee, **then** I can only choose from the project's members.
- **Given** a task exists, **when** I change its details and save, **then** other members see the changes when they next open the project.
- **Given** I'm the project manager, **when** I delete a task and confirm, **then** it's removed from the project.
- **Given** I'm a member but not the project manager, **when** I open a task, **then** I can't delete it.

### US-05: Change a task's status
- **Given** a task is **To Do**, **when** I change it to **In Progress**, **then** other members see it as **In Progress** when they next open the project.
- **Given** a task is **Done**, **when** I change it back to **In Progress**, **then** the task shows **In Progress**.

### US-06: My tasks
- **Given** I have tasks assigned in two projects, **when** I open "My tasks", **then** I see all of them, each with its project name.
- **Given** a task is assigned to someone else, **when** I open "My tasks", **then** it isn't shown.

### US-07: Project board
- **Given** I open a project, **when** the board loads, **then** I see the columns **To Do**, **In Progress** and **Done**, with each task in its status column.
- **Given** a task is in **To Do**, **when** I move it to **Done**, **then** its status changes to **Done**.
- **Given** a task is on the board, **then** its card shows the title, assignee, due date and priority.

### US-08: Highlight late tasks
- **Given** a task was due yesterday and is **In Progress**, **when** I see it on the board or in "My tasks", **then** it's shown with a "Late" label.
- **Given** a late task, **when** its status changes to **Done**, **then** it's no longer shown as late.

### US-09: Delete my account
- **Given** I'm signed in, **when** I delete my account and confirm, **then** I'm signed out and can no longer sign in with that email.
- **Given** my account was deleted, **when** another member opens a project I belonged to, **then** my name and email aren't shown, and tasks that were assigned to me are unassigned.

## 3. Exceptional cases

| ID | Situation | Expected behaviour |
|---|---|---|
| EC-01 | Someone signs up with an email that's already in use. | The account isn't created, and a message explains why. |
| EC-02 | Someone signs in with a wrong email or password. | Sign-in fails with the message "Email or password is incorrect". |
| EC-03 | A project or task is created without a name or title. | It isn't saved, and the empty field is pointed out. |
| EC-04 | A user who isn't a member tries to open a project. | The project isn't shown. |
| EC-05 | A task is due **today**, or has no due date. | It isn't shown as late. |
| EC-06 | A user who is the project manager of a project tries to delete their account. | The account isn't deleted until they delete that project, and a message explains why. |
