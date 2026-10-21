# Matriz

Matriz is a simple shared workspace where a small team plans projects, organises tasks and sees progress in one place.

## The problem

Small teams that track work in spreadsheets and chat lose track of **who is doing what**, and of **what's late**. Finding out how a project is going means asking someone.

## Main goals

1. Everyone can see the status of a project at any time.
2. Everyone knows which tasks are theirs and when they're due.
3. Late tasks are easy to spot.

## MVP features

The MVP is the smallest version where a team can **create a project, assign tasks, move them through the statuses and see what's late**.

| ID | Feature |
|---|---|
| FR-01 | Sign up, sign in and sign out. |
| FR-02 | Create, edit and delete projects. |
| FR-03 | Add and remove project members. |
| FR-04 | Create and edit tasks (title, description, assignee, due date, priority). Only the project manager can delete them. |
| FR-05 | Change a task's status: To Do, In Progress, Done. |
| FR-06 | "My tasks": a list of the tasks assigned to me. |
| FR-07 | A project board with one column per status. |
| FR-08 | Late tasks are highlighted. |
| FR-14 | Delete my account and personal data. |

**Users:** project managers, who create projects and assign tasks, and team members, who create, edit and update tasks. Users only see the projects they belong to.

Features planned for later versions (comments, filters, progress summary, notifications, priority matrix view) and features out of scope are listed in the [requirements](docs/requirements.md).

## Documentation

| Document | Contents |
|---|---|
| [docs/requirements.md](docs/requirements.md) | Vision and goals, user types, functional and non-functional requirements, MVP scope, and features out of the initial scope. |
| [docs/user-stories.md](docs/user-stories.md) | The MVP backlog: 9 user stories (one per MVP requirement) with priorities, Given / When / Then acceptance criteria, and exceptional cases. |
| [docs/constraints.md](docs/constraints.md) | Known constraints, assumptions, security and privacy requirements, pending technical decisions, and scope limitations. |
| [AGENTS.md](AGENTS.md) | Instructions for coding agents working on the project. |

The documents are traceable: each user story (US-xx) is linked to a functional requirement (FR-xx) in the requirements.

## Current status

**Phase: requirements and analysis.** No code has been written yet.

- [x] Problem analysis and clarification
- [x] Requirements and MVP scope
- [x] MVP user stories and acceptance criteria
- [ ] Technical design and technology choice
- [ ] MVP implementation
- [ ] Testing against the acceptance criteria
