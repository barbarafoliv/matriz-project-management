# Matriz: UI/UX Design Decisions

> Companion to [prototype.html](prototype.html). Open that file in a browser to explore the prototype.
>
> The prototype covers the MVP screens: sign in, dashboard, projects with search, project detail with board and filters, task detail page with assignees and comments, My Tasks, and the optional sign-up. Where they differ, this document is the reference.

---

## 1. Screens and navigation

| Screen | Content and actions | FR covered |
|--------|---------------------|------------|
| Sign in | Form, validation and error messages | FR-01 |
| Dashboard | Signed-in user, their projects and each project's task counts per status, with a link to each project | FR-01, FR-17 |
| Projects | Project list, search by name, create, edit, delete and open a project | FR-02, FR-16 |
| Project detail | Project data, board by status, filters by status and priority, new task form, Manage Members | FR-03–08, FR-10 |
| Task detail (page) | Task data, edit, status, priority, assignees, comments and delete | FR-04, FR-05, FR-09, FR-15 |
| My Tasks | Tasks assigned to me in all projects | FR-06 |
| Delete account (modal) | top nav | FR-14 |
| Sign up | Only if the optional FR-18 is built | FR-18 |

**Navigation structure:**
- Unauthenticated users only see Sign in.
- After signing in, the top nav is always visible with the user's name, **Dashboard**, **Projects** and **My Tasks** links, a **Delete account** button and a sign-out button.
- The **dashboard** is the home screen after sign-in (one click to any project).
- **Breadcrumbs** on the project and task detail screens let users go back without the back button.
- When the session has expired, any request sends the user to Sign in with the message "Your session has expired. Sign in again."

---

## 2. Components (reusable)

| Component | Used in |
|-----------|---------|
| `Nav` | All authenticated screens |
| `ProjectCard` (with `StatusCounts`) | Dashboard, Projects screen |
| `SearchBox` | Projects screen |
| `FilterBar` (status, priority) | Project detail |
| `TaskCard` | Board columns |
| `TaskTable` row | My Tasks screen |
| `Badge` (priority, status, late, PM) | Task cards, task detail, My Tasks, member strip |
| `Avatar` | Member strip, task cards, assignees, comments, Manage Members modal |
| `AssigneePicker` | Task detail |
| `CommentList` and `CommentForm` | Task detail |
| `EmptyState` | Dashboard and Projects (no projects or no search results), Board (no tasks or no filter results), My Tasks (none assigned), comments (none yet) |
| `SkeletonLoader` | Dashboard, Projects, Board, task detail, My Tasks — loading state |
| `ErrorBanner` | Dashboard, Projects, Board, task detail, My Tasks — error state |
| `Modal` | New Project, Edit Project, Manage Members, confirmations, Delete Account |

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
**Decision:** three segmented buttons (To Do / In Progress / Done) on the task detail page.  
**Why:** there are exactly three fixed statuses. A button group shows all options at a glance and requires one click instead of two.

### "Manage Members" shown only to the project manager (US-03)
**Decision:** the button is hidden for non-PM members.  
**Why:** only the PM can add or remove members. Showing the button to members would require a subsequent 403, which is a worse experience than not showing it.

### Delete task shown only to the project manager, on the task detail page (US-04)
**Decision:** there is no delete button on the task card, and the button on the task detail page is hidden for non-PM members.  
**Why:** deletion is a destructive, low-frequency action. Requiring the user to open the task first adds one step and avoids accidental deletion on the board.

### Status buttons save immediately (US-05)
**Decision:** clicking a status button on the task detail page saves the new status straight away. **Save** applies the other fields.  
**Why:** the API changes the status through its own endpoint (`PUT .../status`), separate from editing the task's details.

### "My Tasks" shows all projects in one flat list, not grouped by project (FR-06, US-06)
**Decision:** flat table sorted by due date, with a "Project" column.  
**Why:** the primary goal is to see what is assigned to me and what is late. Grouping by project makes it harder to spot the most urgent work across projects.

### Task detail is a page, not a modal (FR-04, FR-09, FR-15)
**Decision:** clicking a task card opens the task detail page, with its own route.  
**Why:** the task now has assignees and comments as well as its details. A page has room for all of them, and can be reopened or linked directly.

### Assignees added one at a time (US-10, EC-08, EC-09)
**Decision:** the task detail page lists the assignees, each with a remove button, and an `AssigneePicker` that only offers project members who aren't assigned yet. Task cards show up to 3 assignee initials, then "+N".  
**Why:** the API adds and removes one assignment at a time. Offering only eligible members avoids most errors; the API still rejects duplicates and non-members.

### Comments listed oldest first, below the task details (US-11)
**Decision:** comments show the author's name (or "Deleted user"), the date and time, and the text. The form is below the list, and the new comment is added from the API response.  
**Why:** reading from oldest to newest follows the conversation. Using the API response shows the author and date set by the server.

### Filters above the board (US-13)
**Decision:** status and priority dropdowns above the board, each with an "All" option. Changing a filter reloads the tasks from the API.  
**Why:** the API filters the tasks, so the board shows exactly what matches. Filters don't change the dashboard counts.

### Project search on the Projects screen (US-12)
**Decision:** a search box above the project list. Searching asks the API after the user stops typing, and an empty search shows all projects.  
**Why:** the API searches the caller's projects only, so other users' projects are never shown.

### No progress summary (FR-11)
**Decision:** not implemented.  
**Why:** FR-11 is a "Should" requirement outside the MVP scope.

### Refreshing after changes
**Decision:** after creating, editing or deleting, the screen updates from the API response or reloads the affected list. Submit buttons are disabled while a request is in progress, and a failed request keeps the form's values.  
**Why:** lists, filters and counts stay correct without a page reload, and records aren't sent twice.

### Clicking a task in My Tasks navigates to the board and highlights it (Q1)
**Decision:** the row navigates to the project board and applies a visible highlight (outline ring + tint) to the task card, scrolled into view. No modal opens automatically.  
**Why:** the board gives full context (column, neighbouring tasks, project members). The highlight tells the user exactly which task they came from. The task detail page can still be opened by clicking the card.

### Confirmation modals for destructive actions (Q4)
**Decision:** Delete project, Delete task, Remove member and Delete account each open a confirmation modal before acting.  
**Why:** US-02, US-04 and US-09 explicitly say "and confirm". The remove modal names the affected member so the consequence (removal of their task assignments) is clear before the user commits.

### Delete account blocked while the user manages a project (US-09, EC-06)
**Decision:** if the user is the project manager of any project, the Delete Account modal lists those projects and has no Delete button.  
**Why:** EC-06 requires the account to stay until those projects are deleted, with a message that explains why.

### Project cards show task counts per status (FR-17)
**Decision:** project cards show the name, description, role and the number of tasks To Do, In Progress and Done, each with its label (not colour alone, NFR-05).  
**Why:** FR-17 asks for them. The API returns `taskCounts` with each project, counted over all of its tasks.

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
| User is already assigned (EC-08) | `ALREADY_ASSIGNED` | "This person is already assigned to the task." | Task detail, assignees |
| Assignee isn't a project member (EC-09) | `VALIDATION_FAILED` (`NOT_A_MEMBER`, `USER_NOT_FOUND`) | "Only project members can be assigned." | Task detail, assignees |
| Empty or too long comment (EC-10) | `VALIDATION_FAILED` | The field's message, e.g. "Comment must not be empty." | Under the comment field |
| No search or filter results (EC-11) | — | "No projects match your search." / "No tasks match these filters." | Empty state |
| Session expired | `UNAUTHENTICATED` | "Your session has expired. Sign in again." | Sign in screen |

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
- The task detail page is the single place to edit all task fields, assignees and comments.
- Clicking a task card on the board opens the task detail page.
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
