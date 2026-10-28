# Matriz: User Stories (MVP)

> Based on the MVP scope in [requirements.md](requirements.md): one user story per *Must* functional requirement (FR-01 to FR-10 and FR-14 to FR-17).

**Statuses:** To Do, In Progress, Done. A new task starts as **To Do**, with priority **Medium**.
**Late task:** due date before today (in the user's local date) and status not **Done**.
**Eligible assignee:** a member of the task's project, including the project manager.

## 1. User stories and priorities

All stories are part of the MVP (**Must**). The order is the order in which they should be built.

| Order | ID | User story | Requirement | Priority |
|---|---|---|---|---|
| 1 | US-01 | As a **user**, I want to sign in and sign out, and see who is signed in, so that my work is linked to my account. | FR-01 | Must |
| 2 | US-02 | As a **project manager**, I want to create, edit and delete projects, so that the team's work is organised in one place. | FR-02 | Must |
| 3 | US-12 | As a **user**, I want to search my projects by name, so that I find a project quickly. | FR-16 | Must |
| 4 | US-03 | As a **project manager**, I want to add and remove project members, so that only the right people work on the project. | FR-03 | Must |
| 5 | US-04 | As a **project member**, I want to create, open and edit tasks, so that the work to be done is recorded. Only the **project manager** can delete them. | FR-04 | Must |
| 6 | US-05 | As a **project member**, I want to change a task's status, so that everyone knows how the work is progressing. | FR-05 | Must |
| 7 | US-07 | As a **project member**, I want to see the project's tasks on a board by status, so that I see the progress at a glance. | FR-07 | Must |
| 8 | US-13 | As a **project member**, I want to filter a project's tasks by status and priority, so that I find the work I'm looking for. | FR-10 | Must |
| 9 | US-08 | As a **project member**, I want late tasks to be highlighted, so that late work is easy to spot. | FR-08 | Must |
| 10 | US-10 | As a **project member**, I want to assign one or more members to a task, and remove them, so that everyone knows who is doing it. | FR-15 | Must |
| 11 | US-06 | As a **team member**, I want to see the tasks assigned to me, so that I know what I have to do. | FR-06 | Must |
| 12 | US-11 | As a **project member**, I want to comment on a task and read its comments, so that the discussion stays next to the work. | FR-09 | Must |
| 13 | US-14 | As a **user**, I want a dashboard with my projects and their task counts per status, so that I see how all my projects are going. | FR-17 | Must |
| 14 | US-09 | As a **user**, I want to delete my account, so that my personal data is removed. | FR-14 | Must |

## 2. Acceptance criteria

### US-01: Sign in and sign out
- **Given** I have an account, **when** I sign in with the correct email and password, **then** I see the dashboard with my name.
- **Given** I'm signed in, **when** I sign out, **then** I'm sent to the sign-in screen.
- **Given** I'm not signed in, **when** I try to open a project, **then** I'm sent to the sign-in screen.
- **Given** my session has expired, **when** I do anything, **then** I'm sent to the sign-in screen with the message "Your session has expired. Sign in again."
- **Given** the application has just been installed, **when** the seed has run, **then** I can sign in with the demo accounts listed in the README.

### US-02: Create, edit and delete projects
- **Given** I'm signed in, **when** I create a project with a name and, optionally, a description, **then** it appears in my project list and I'm its project manager.
- **Given** I'm a member of a project, **when** I open it, **then** I see its name, description and tasks.
- **Given** I'm the project manager, **when** I change the project's name and save, **then** other members see the new name when they next open the project.
- **Given** I'm the project manager, **when** I delete the project and confirm, **then** the project and its tasks, assignments and comments no longer appear anywhere.
- **Given** I'm a member but not the project manager, **when** I open the project, **then** I can't edit or delete it.

### US-12: Search projects by name
- **Given** I'm a member of "Website redesign" and "Mobile launch", **when** I search for "web", **then** I only see "Website redesign" (ignoring upper and lower case).
- **Given** another user has a project called "Website", **when** I search for "web", **then** their project isn't shown.
- **Given** I clear the search, **then** I see all my projects again.

### US-03: Add and remove project members
- **Given** I'm the project manager, **when** I add a registered user by email, **then** the project appears in their project list.
- **Given** I'm the project manager, **when** I remove a member, **then** they can no longer see the project, and they're removed from the tasks they were assigned to in that project.

### US-04: Create, open and edit tasks
- **Given** I'm a project member, **when** I create a task with a title, **then** it's added to the project with status **To Do** and priority **Medium**, unless I choose another priority, and it appears without reloading the page.
- **Given** a task exists, **when** I open it, **then** I see its title, description, due date, priority, status, assignees and comments.
- **Given** a task exists, **when** I change its details and save, **then** other members see the changes when they next open the project.
- **Given** I'm the project manager, **when** I delete a task and confirm, **then** it's removed from the project, with its assignments and comments.
- **Given** I'm a member but not the project manager, **when** I open a task, **then** I can't delete it.

### US-05: Change a task's status
- **Given** a task is **To Do**, **when** I change it to **In Progress**, **then** other members see it as **In Progress** when they next open the project.
- **Given** a task is **Done**, **when** I change it back to **In Progress**, **then** the task shows **In Progress**.

### US-07: Project board
- **Given** I open a project, **when** the board loads, **then** I see the columns **To Do**, **In Progress** and **Done**, with each task in its status column.
- **Given** a task is in **To Do**, **when** I move it to **Done**, **then** its status changes to **Done**.
- **Given** a task is on the board, **then** its card shows the title, assignees, due date and priority.

### US-13: Filter tasks by status and priority
- **Given** a project has tasks with different priorities, **when** I filter by **High**, **then** I only see the high-priority tasks.
- **Given** I filter by **In Progress** and **High**, **then** I only see tasks that match both.
- **Given** a filter is applied, **when** I change a task so it no longer matches, **then** it disappears from the filtered list without reloading the page.

### US-08: Highlight late tasks
- **Given** a task was due yesterday and is **In Progress**, **when** I see it on the board or in "My tasks", **then** it's shown with a "Late" label.
- **Given** a late task, **when** its status changes to **Done**, **then** it's no longer shown as late.

### US-10: Assign members to a task
- **Given** I'm a project member, **when** I assign two members to a task, **then** both are shown as its assignees.
- **Given** I'm choosing an assignee, **then** I can only choose from the project's members.
- **Given** a task has two assignees, **when** I remove one, **then** only the other is shown.
- **Given** I assign members and close the task, **when** I open it again, **then** the same assignees are shown.

### US-06: My tasks
- **Given** I have tasks assigned in two projects, **when** I open "My tasks", **then** I see all of them, each with its project name.
- **Given** a task is assigned to me and someone else, **when** I open "My tasks", **then** it's shown.
- **Given** a task is assigned only to someone else, **when** I open "My tasks", **then** it isn't shown.

### US-11: Comment on tasks
- **Given** I'm a project member, **when** I add a comment to a task, **then** it's shown with my name and the date and time, without reloading the page.
- **Given** a task has comments, **when** I open it, **then** I see them from oldest to newest, each with its author and date.

### US-14: Dashboard
- **Given** I sign in, **when** the dashboard loads, **then** I see my name and my projects, each with its number of tasks **To Do**, **In Progress** and **Done**.
- **Given** I change a task's status, **when** I go back to the dashboard, **then** the counts reflect the change.
- **Given** I click a project on the dashboard, **then** its detail page opens.

### US-09: Delete my account
- **Given** I'm signed in, **when** I delete my account and confirm, **then** I'm signed out and can no longer sign in with that email. The email can be used by a new account.
- **Given** my account was deleted, **when** another member opens a project I belonged to, **then** my name and email aren't shown, I'm no longer an assignee of any task, and my comments are shown as written by "Deleted user".

## 3. Exceptional cases

| ID | Situation | Expected behaviour |
|---|---|---|
| EC-01 | Someone signs up with an email that's already in use, ignoring upper and lower case (only if FR-18 is built). | The account isn't created, and a message explains why. |
| EC-02 | Someone signs in with a wrong email or password. | Sign-in fails with the message "Email or password is incorrect". |
| EC-03 | A project is saved with a name shorter than 3 characters, or a task with a title shorter than 3 characters, or either is made only of spaces. | It isn't saved, the field is pointed out, and the rest of the form is kept. |
| EC-04 | A user who isn't a member tries to open a project, or one of its tasks or comments, even by changing the ID in the request. | The project or task isn't shown, as if it didn't exist. |
| EC-05 | A task is due **today**, or has no due date. | It isn't shown as late. |
| EC-06 | A user who is the project manager of a project tries to delete their account. | The account isn't deleted until they delete that project, and a message explains why. |
| EC-07 | The project manager tries to remove themselves from their project. | They aren't removed, and a message explains why. |
| EC-08 | A member is assigned to a task they're already assigned to. | No second assignment is created, and a message explains why. |
| EC-09 | Someone tries to assign a user who doesn't exist or isn't a project member. | The assignment isn't made, and a message explains why. |
| EC-10 | A comment is empty, made only of spaces, or longer than 2000 characters. | It isn't saved, and the field is pointed out. |
| EC-11 | A project search or a task filter matches nothing. | An empty list is shown with a message, not an error. |
