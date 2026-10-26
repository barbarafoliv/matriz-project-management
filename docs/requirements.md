# Matriz: Requirements

**Priorities:** **Must** = in the MVP · **Should** = next version · **Could** = maybe later.

## 1. Vision and goals

**Vision:** Matriz is a simple shared workspace where a small team plans projects, organises tasks and sees progress in one place.

**Problem**: small teams that track work in spreadsheets and chat lose track of who is doing what, and of what's late.

**Goals:**
1. Everyone can see the status of a project at any time.
2. Everyone knows which tasks are theirs and when they're due.
3. Late tasks are easy to spot.

## 2. User types

| User | What they do |
|---|---|
| **Project manager** | Creates projects, adds members, creates and assigns tasks, follows progress. |
| **Team member** | Creates and edits tasks, and updates their status. |

These are roles in each project, not types of account: a user can be the project manager of one project and a team member of another.

Users only see the projects they are members of.

## 3. Functional requirements

| ID | Requirement | Why | Priority |
|---|---|---|---|
| FR-01 | Users can sign up, sign in and sign out. | Each person needs an account to own tasks. | Must |
| FR-02 | A project manager can create, edit and delete projects (name and optional description). | Projects organise the work. | Must |
| FR-03 | A project manager can add and remove project members. | Controls who works on what. | Must |
| FR-04 | Members can create and edit tasks (title, description, assignee, due date, priority: Low, Medium or High, Low by default). Only the project manager can delete tasks. | Tasks are the core of the app. | Must |
| FR-05 | Members can change a task's status (To Do, In Progress, Done). | Shows progress (goal 1). | Must |
| FR-06 | Users can see a list of tasks assigned to them ("My tasks"). | One place to see your own work (goal 2). | Must |
| FR-07 | Each project has a board with one column per status. | Progress at a glance (goal 1). | Must |
| FR-08 | Late tasks are highlighted. | Late work is easy to spot (goal 3). | Must |
| FR-09 | Members can comment on tasks. | Keeps the discussion next to the work. | Should |
| FR-10 | Tasks can be filtered by status, assignee and priority. | Easier to find work in bigger projects. | Should |
| FR-11 | Each project shows a progress summary (% of tasks done, number of late tasks). | Quick project overview (goals 1 and 3). | Should |
| FR-12 | Users get a notification when a task is assigned to them. | People know when they have new work. | Could |
| FR-13 | A priority matrix view that groups a user's tasks by **urgency** (due date) and **importance** (priority). | Helps members decide what to work on first (goals 2 and 3). | Could |
| FR-14 | Users can delete their account and personal data. | Required by the GDPR. | Must |

## 4. Non-functional requirements

| ID | Requirement | Priority |
|---|---|---|
| NFR-01 | **Usability:** a new user can create a project and a task without help. | Must |
| NFR-02 | **Security:** only signed-in users can use the app, and they only see their own projects. Passwords are stored securely, repeated failed sign-in attempts are limited, and inactive sessions expire. Password rules and exact limits are set in the technical design. | Must |
| NFR-03 | **Reliability:** saved data isn't lost. | Must |
| NFR-04 | **Performance:** main screens load in about 2 seconds. | Should |
| NFR-05 | **Accessibility:** status and late tasks aren't shown by colour alone. | Should |

## 5. MVP scope

The MVP is the smallest version where a team can **create a project, assign tasks, move them through the statuses and see what's late**.

- **Included:** all *Must* requirements, FR-01 to FR-08, FR-14 and NFR-01 to NFR-03.
- **Done when:** a project manager creates a project, adds two members and assigns them tasks. The members see their tasks, move them across the board, and late tasks are highlighted.

## 6. Out of the initial scope

| Feature | Why not now |
|---|---|
| Several organisations or workspaces | One shared workspace per team is enough for a first version. |
| Custom statuses and workflows | The fixed statuses are enough. |
| Subtasks and dependencies between tasks | Adds complexity without being essential. |
| File attachments | Not needed to manage tasks. |
| Calendar and Gantt views | The board and the lists cover the basics. |
| Email notifications, real-time chat | Too complex for a first version. |
| Time tracking and reports | A different focus from task management. |
| Mobile app and other languages | Desktop and English are enough for now. |
