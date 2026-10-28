# Matriz: UI/UX Design Decisions

> Companion to [prototype.html](prototype.html). Open that file in a browser to explore the prototype.

---

## 1. Screens and navigation

| Screen | Route (prototype) | FR covered |
|--------|-------------------|------------|
| Sign in | `sign-in` | FR-01 |
| Sign up | `sign-up` | FR-01 |
| My Projects (dashboard) | `projects` | FR-02 |
| Project Board | `board` | FR-04–08 |
| My Tasks | `my-tasks` | FR-06 |
| Delete account (modal) | top nav | FR-14 |

**Navigation structure:**
- Unauthenticated users see only Sign in ↔ Sign up.
- After signing in, the top nav is always visible with a **My Tasks** link, a **Delete account** button and a sign-out button.
- The **project list** is the home screen after sign-in (one click to any project board).
- **Breadcrumb** on the board screen lets users return to the project list without the back button.

---

## 2. Components (reusable)

| Component | Used in |
|-----------|---------|
| `Nav` | All authenticated screens |
| `ProjectCard` | Projects screen |
| `TaskCard` | Board columns |
| `TaskTable` row | My Tasks screen |
| `Badge` (priority, status, late, PM) | Task cards, My Tasks, member strip |
| `Avatar` | Member strip, task cards, Manage Members modal |
| `EmptyState` | Projects (no projects), Board (no tasks), My Tasks (none assigned) |
| `SkeletonLoader` | Projects, Board, My Tasks — loading state |
| `ErrorBanner` | Projects, Board, My Tasks — error state |
| `Modal` | New Project, Edit Project, New Task, Task Detail, Manage Members, Delete Account |

---

## 3. UI/UX decisions

### Late task indicator (FR-08, EC-05, NFR-05)
**Decision:** late tasks have a thick left border (3 px, grey) **and** a "Late" text badge — never colour alone.  
**Why:** NFR-05 requires that status is not communicated by colour alone. A border + label works for users with colour-vision deficiencies and on monochrome displays.

### 403 and 404 merged into "Project not found" (EC-04)
**Decision:** non-members who try to open a project see "Project not found", not "Forbidden".  
**Why:** revealing whether a project exists leaks information. Consistent with the API contract in [openapi.yaml](../openapi.yaml).

### Project manager role shown with a "PM" badge, not a separate user type
**Decision:** PM badge on project cards and in the member strip, not a separate account type.  
**Why:** a user can be PM of one project and a member of another (requirements §2). A separate account type would be wrong.

### Status change as a button group, not a dropdown (US-05)
**Decision:** three segmented buttons (To Do / In Progress / Done) in the task detail modal.  
**Why:** there are exactly three fixed statuses. A button group shows all options at a glance and requires one click instead of two.

### "Manage Members" shown only to the project manager (US-03)
**Decision:** the button is hidden for non-PM members.  
**Why:** only the PM can add or remove members. Showing the button to members would require a subsequent 403, which is a worse experience than not showing it.

### Delete task shown only to the project manager, in the task detail modal (US-04)
**Decision:** there is no delete button on the task card, and the button in the task detail modal is hidden for non-PM members.  
**Why:** deletion is a destructive, low-frequency action. Requiring the user to open the task first adds one step and avoids accidental deletion on the board.

### Status buttons save immediately (US-05)
**Decision:** clicking a status button in the task detail modal saves the new status straight away. **Save** applies the other fields.  
**Why:** the API changes the status through its own endpoint (`PUT .../status`), separate from editing the task's details.

### "My Tasks" shows all projects in one flat list, not grouped by project (FR-06, US-06)
**Decision:** flat table sorted by due date, with a "Project" column.  
**Why:** the primary goal is to see what is assigned to me and what is late. Grouping by project makes it harder to spot the most urgent work across projects.

### No filters, progress summary, or comments in prototype (FR-09–11)
**Decision:** not implemented.  
**Why:** these are "Should" requirements outside the MVP scope. The prototype covers only Must requirements.

### Clicking a task in My Tasks navigates to the board and highlights it (Q1)
**Decision:** the row navigates to the project board and applies a visible highlight (outline ring + tint) to the task card, scrolled into view. No modal opens automatically.  
**Why:** the board gives full context (column, neighbouring tasks, project members). The highlight tells the user exactly which task they came from. The modal can still be opened by clicking the card.

### Confirmation modals for destructive actions (Q4)
**Decision:** Delete project, Delete task, Remove member and Delete account each open a confirmation modal before acting.  
**Why:** US-02, US-04 and US-09 explicitly say "and confirm". The remove modal names the affected member so the consequence (task unassignment) is clear before the user commits.

### Delete account blocked while the user manages a project (US-09, EC-06)
**Decision:** if the user is the project manager of any project, the Delete Account modal lists those projects and has no Delete button.  
**Why:** EC-06 requires the account to stay until those projects are deleted, with a message that explains why.

### Project cards show only name, description and role
**Decision:** no member or task counts on project cards.  
**Why:** no MVP requirement asks for them, and the API doesn't return them. Progress summaries are FR-11, a *Should* requirement.

### Error messages
| Situation | Code (API) | Message | Shown in |
|---|---|---|---|
| Email already in use (EC-01) | `EMAIL_IN_USE` | "An account with this email already exists." | Sign up form |
| Wrong email or password (EC-02) | `INVALID_CREDENTIALS` | "Email or password is incorrect." | Sign in form |
| Too many failed sign-ins | `TOO_MANY_SIGN_IN_ATTEMPTS` | "Too many failed sign-in attempts. Try again in 15 minutes." | Sign in form |
| Empty or invalid field (EC-03) | `VALIDATION_FAILED` | The field's message, e.g. "Title must not be empty." | Under the field |
| Not a member, or no such project (EC-04) | `PROJECT_NOT_FOUND` | "Project not found." | Instead of the board |
| User manages a project (EC-06) | `ACCOUNT_MANAGES_PROJECTS` | "Delete the projects you manage before deleting your account." | Delete Account modal |
| Unknown email when adding a member | `VALIDATION_FAILED` (`USER_NOT_FOUND`) | "No account found with this email." | Manage Members |
| User is already a member | `ALREADY_MEMBER` | "This person is already a member." | Manage Members |
| Session expired | `UNAUTHENTICATED` | — (redirect to Sign in) | — |

---

## 4. Accessibility checklist

| Item | Implemented |
|------|-------------|
| Skip-to-content link | ✓ |
| Semantic landmarks (`<nav>`, `<main>`, `<section>`, `<article>`) | ✓ |
| Heading hierarchy (h1 per screen, h2 in modals) | ✓ |
| `aria-label` on icon-only buttons, avatars, badge annotations | ✓ |
| `aria-current="page"` on active nav link | ✓ |
| `aria-busy="true"` during loading states | ✓ |
| `aria-pressed` on status toggle buttons | ✓ |
| `role="dialog" aria-modal="true"` on modals | ✓ |
| `role="alert"` on error messages | ✓ |
| `role="status"` on empty states | ✓ |
| Focus-visible ring on all interactive elements | ✓ |
| Colour is never the only indicator (Late badge has text + border) | ✓ |
| Form inputs have explicit `<label>` elements | ✓ |
| Required fields have `aria-required="true"` | ✓ |

---

## 5. Responsive behaviour

- **Project grid:** `auto-fill` with `minmax(270px, 1fr)` — wraps naturally on narrow screens.
- **Board columns:** `overflow-x: auto` on wide viewports; stacked vertically below 600 px.
- **My Tasks table:** `overflow-x: auto` wrapper prevents overflow on small screens.
- **Modals:** `max-width: 470px`, `max-height: 90vh` with scroll — work on phones.
- **Two-column form grids:** collapse to one column below 600 px.

---

## 6. Assumptions

- The Angular application will follow this component structure; specific Angular component names are a later decision.
- Avatars show the user's initials (first letter of first and last name). No photo upload is in the MVP.
- The task detail modal is the single place to edit all task fields. There is no separate "edit task" page.
- Clicking a task card on the board opens the task detail modal (not a separate page).
- The order of tasks within a board column is by due date, tasks without one last. No drag-and-drop in the MVP.

---

## 7. Decisions (resolved)

All four open questions have been answered and implemented:

| # | Question | Decision |
|---|----------|----------|
| Q1 | My Tasks → board or modal? | Navigate to board and highlight the task (outline ring + tint + scroll). |
| Q2 | Full name or initials on task cards? | Initials only — already implemented. |
| Q3 | Sign-up auto-sign-in? | Yes — auto-signs the user in and goes to the Projects screen. |
| Q4 | Confirmation before delete/remove? | Yes — confirmation modal for each destructive action. |
