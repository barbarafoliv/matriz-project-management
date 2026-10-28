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
| **Team member** | Creates and edits tasks, assigns them, updates their status and comments on them. |

These are roles in each project, not types of account: a user can be the project manager of one project and a team member of another.

Users only see the projects they are members of.

## 3. Functional requirements

| ID | Requirement | Why | Priority |
|---|---|---|---|
| FR-01 | Users can sign in and sign out, and see who is signed in. Demo accounts are created by a seed. | Each person needs an account to own tasks. | Must |
| FR-02 | A project manager can create, edit and delete projects (name and optional description). Members can list and open their projects. | Projects organise the work. | Must |
| FR-03 | A project manager can add and remove project members. | Controls who works on what. | Must |
| FR-04 | Members can create, open and edit tasks (title, description, due date, priority: Low, Medium or High, Medium by default). Only the project manager can delete tasks. | Tasks are the core of the app. | Must |
| FR-05 | Members can change a task's status (To Do, In Progress, Done). | Shows progress (goal 1). | Must |
| FR-06 | Users can see a list of tasks assigned to them ("My tasks"). | One place to see your own work (goal 2). | Must |
| FR-07 | Each project has a board with one column per status. | Progress at a glance (goal 1). | Must |
| FR-08 | Late tasks are highlighted. | Late work is easy to spot (goal 3). | Must |
| FR-09 | Members can add comments to a task and see its comments, each with its author and date. | Keeps the discussion next to the work. | Must |
| FR-10 | Tasks can be filtered by status and priority. | Easier to find work in bigger projects. | Must |
| FR-11 | Each project shows a progress summary (% of tasks done, number of late tasks). | Quick project overview (goals 1 and 3). | Should |
| FR-12 | Users get a notification when a task is assigned to them. | People know when they have new work. | Could |
| FR-13 | A priority matrix view that groups a user's tasks by **urgency** (due date) and **importance** (priority). | Helps members decide what to work on first (goals 2 and 3). | Could |
| FR-14 | Users can delete their account and personal data. | Required by the GDPR. | Must |
| FR-15 | Members can assign several project members to a task, and remove each assignment. | Work is often shared (goal 2). | Must |
| FR-16 | Users can search their projects by name. | Easier to find a project. | Must |
| FR-17 | The home screen (dashboard) shows the signed-in user, their projects and each project's number of tasks per status, with a link to each project. | Progress of all projects at a glance (goal 1). | Must |
| FR-18 | Anyone can sign up for an account. | Lets new people join without a seed. | Could |
| FR-19 | Users can reset a forgotten password. | Users can recover their account. | Could |
| FR-20 | Tasks can be filtered by assignee. | Easier to find someone's work in a project. | Should |

## 4. Non-functional requirements

| ID | Requirement | Priority |
|---|---|---|
| NFR-01 | **Usability:** a new user can create a project and a task without help. | Must |
| NFR-02 | **Security:** only signed-in users can use the app, and they only see their own projects. Passwords are stored securely, repeated failed sign-in attempts are limited, and inactive sessions expire. Password rules and exact limits are set in the technical design. | Must |
| NFR-03 | **Reliability:** saved data isn't lost. | Must |
| NFR-04 | **Performance:** main screens load in about 2 seconds. | Should |
| NFR-05 | **Accessibility:** status and late tasks aren't shown by colour alone. | Should |

## 5. MVP scope

The MVP is the smallest version where a team can **create a project, assign tasks, discuss them, move them through the statuses and see what's late**.

- **Included:** all *Must* requirements, FR-01 to FR-10, FR-14 to FR-17 and NFR-01 to NFR-03.
- **Done when:** a demo project manager signs in, creates a project, adds two members and assigns tasks to them, some to both. The members see their tasks, comment on them, move them across the board and filter them, late tasks are highlighted, and the dashboard counts match the tasks.

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
