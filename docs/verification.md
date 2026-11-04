---
title: Verification
---

# Matriz: Verification

> Results of checking the MVP against [requirements.md](requirements.md) and [user-stories.md](user-stories.md) (Practical Sheet 03, Exercises 8 and 9, and Practical Worksheet 04). Final run on 2026-11-04, on Windows 11 with Docker (Rancher Desktop), JDK 21, Node.js 24 and Chromium.

## 1. Commands and results

| Command | Where | Result |
|---|---|---|
| `./mvnw verify` | `backend/` | 100 tests, 0 failures (PostgreSQL 18 in Testcontainers) |
| `npx ng test --watch=false` | `frontend/` | 92 tests, 0 failures |
| `npx ng build` | `frontend/` | Build complete, no warnings |
| `npm run lint` | `frontend/` | All files pass linting |
| `./mvnw compile spotbugs:check` | `backend/` | No findings (SpotBugs with FindSecBugs) |
| `npm audit`; OSV-Scanner | `frontend/`, `backend/` | 0 known vulnerabilities (frontend 649 packages, backend 165) |
| `E2E_PASSWORD=… npm run e2e` | `frontend/`, app running | 1 test passed, 3 runs in a row; no data left behind |
| Access between demo accounts (`curl`) | running app | 14 × 404 for a non-member, 4 × 403 for a member who isn't the PM, data unchanged ([quality-security-review.md](quality-security-review.md), section 3) |
| README steps from a clean clone | temporary folder | Passed: schema and demo data created, health `db: UP`, Alice signs in with the README password, app and `/api` proxy work; 82 backend and 79 frontend tests passed at commit `ff93f3d` (before the review fixes, which added 6 tests) |
| Contract against code | `docs/openapi.yaml`, controllers | 25 operations (version 1.0.0); all implemented except `POST /users` (optional sign-up, FR-18) |

## 2. Cases

**A** = automated test (class or file, then test). **M** = manual check in the browser, done with each increment (details in [implementation-plan.md](implementation-plan.md)).

| # | Case | Expected | Observed | Evidence |
|---|---|---|---|---|
| 01 | Access to the application | Valid credentials sign in; wrong ones and requests without a session are refused | As expected. Wrong email and wrong password give the same message; the 6th failed attempt gets 429 with `Retry-After`; no session gives 401; an expired session sends the user to sign-in with a message | **A** `SignInTests` (16), `SessionCookieTests`; `sign-in.spec`, `auth.guards.spec`, `session-expired.interceptor.spec`. **M** Increments 2 and 3 |
| 02 | Project management | Create, search, edit and delete follow the permissions | As expected. Only members see a project; only the PM edits, deletes and manages members; search ignores case | **A** `ProjectTests` (13), `MemberTests` (10); `projects-page.spec`, `project-form-dialog.spec`, `project-detail.spec`, `members-dialog.spec`. **M** Increment 3 |
| 03 | Task persistence | A task appears without reloading and is still there after restarting the backend | As expected | **A** `TaskTests.createsATaskAsToDoWithMediumPriorityByDefault` (reads back from the database); `task-board.spec` (reload after create). **M** Increment 4, backend restarted |
| 04 | Data validation | Empty fields, fields made only of spaces and invalid values are rejected | As expected, with one error per field and nothing saved | **A** `TaskTests.rejectsATitleMadeOnlyOfSpacesAndSavesNothing`, `TaskTests.rejectsOtherInvalidFields`, `ProjectTests.rejectsInvalidNamesAndDescriptions`, `CommentTests.rejectsEmptyBlankAndTooLongComments`, `SignInTests.reportsInvalidFields`, `SchemaAndSeedTests.rejectsATitleMadeOnlyOfSpaces`; form specs. **M** Increments 3, 4 and 6 |
| 05 | Status and priority | Editing updates the task, the filters and the counts consistently | As expected. Late is cleared when a task is done; counts follow status changes | **A** `TaskTests.changesTheStatusInAnyDirection`, `filtersByStatusAndPriority`, `marksLateTasksUsingTodaysDate`, `projectCountsFollowStatusChanges`; `task-detail.spec`, `task-board.spec`, `dashboard.spec`. **M** Increments 4 and 7 |
| 06 | Assignees | Assigning and removing work; the same assignment isn't duplicated | As expected. Only project members can be assigned | **A** `AssigneeTests` (8), `SchemaAndSeedTests.rejectsADuplicateAssignment`; `assignees-panel.spec`, `my-tasks-page.spec`. **M** Increment 5 |
| 07 | Comments | The content, the signed-in author and the date are saved and shown | As expected. An author or date sent in the request is ignored | **A** `CommentTests` (6), including `ignoresAnAuthorOrDateSentInTheRequest`; `comments-panel.spec`. **M** Increment 6, backend restarted |
| 08 | Authorisation in the API | Another user can't see or change resources, even by changing the IDs in the request | As expected: 404 `PROJECT_NOT_FOUND` or `TASK_NOT_FOUND`, data unchanged | **A** `ProjectTests.hidesProjectsFromNonMembersEvenWhenTheIdIsChanged`, `TaskTests.nonMembersCannotSeeOrChangeTasksEvenWhenTheIdsAreChanged`, `TaskTests.aTaskCanOnlyBeReachedThroughItsOwnProject`, `AssigneeTests.nonMembersCannotAssignOrUnassignEvenWhenTheIdsAreChanged`, `CommentTests.nonMembersCannotReadOrAddCommentsEvenWhenTheIdsAreChanged`, `SignInTests.rejectsASignInWithoutTheCsrfToken`. **M** Increments 3 to 5 (Carol opening Alice's URLs) |
| 09 | Deletion and errors | Deletion respects dependencies; API errors don't show success or lose form data | As expected. Deleting a project or task leaves no orphan rows; forms keep their values after an error | **A** `ProjectTests.deletingAProjectDeletesItsTasksMembershipsAndAssignments`, `TaskTests.onlyTheProjectManagerCanDeleteATask`, `AccountDeletionTests` (7); `project-form-dialog.spec`, `new-task-dialog.spec`, `task-detail.spec`, `comments-panel.spec` ("keeps …" tests). **M** Increments 3, 4 and 8 |

The three tests asked for by the sheet: validation, `TaskTests.rejectsATitleMadeOnlyOfSpacesAndSavesNothing`; authorisation, `ProjectTests.hidesProjectsFromNonMembersEvenWhenTheIdIsChanged`; creation read back from the database, `ProjectTests.createsAProjectThatIsSavedAndListed`.

## 3. Bugs found and fixed

Each was reproduced, its cause found, fixed in a limited way and covered by a regression test.

| Found in | Problem | Cause | Regression test |
|---|---|---|---|
| Increment 2, browser | Sign-in always failed with 403 | The `XSRF-TOKEN` cookie had `Path=/api`, so the app couldn't read it | `SessionCookieTests` (cookie path) |
| Increment 2, browser | Every anonymous request created a session | Rejected requests were saved in a session for later | `SessionCookieTests` (no `SESSION` before sign-in) |
| Increment 3, browser | Signing in again right after signing out failed with 403 | Sign-out removes the CSRF token and the app didn't ask for a new one | `auth.service.spec` |
| Increment 3, tests | Invalid member email reported as a malformed request | Body and path validation errors arrive in the same exception | `MemberTests.rejectsAnInvalidEmail` |
| Increment 5, browser | The assignee picker started blank and Assign chose someone silently | No placeholder option | `assignees-panel.spec` |
| Review | Deleting the account with an expired session showed no "session expired" message | The interceptor ignored all 401s for `/users/me` | `session-expired.interceptor.spec` |
| Review | Changing filters quickly could show tasks of the previous filter | An older response could arrive last | `task-board.spec` |
| Review | Two simultaneous "add member" requests could give 500 | The primary key error wasn't handled | `MemberTests.simultaneousRequestsToAddTheSameMemberCreateOneMembershipAndNoServerError` (best effort: it can't force the race) |
| Review | A description at the limit plus a trailing space was rejected | Length checked before trimming | `ProjectTests` and `TaskTests.checksTheDescriptionLengthAfterTrimming` |
| Worksheet 04 review | A deletion that wasn't committed still signed the user out everywhere | Spring Session commits session changes on its own | `AccountDeletionTests.keepsTheSessionsWhenTheDeletionIsNotCommitted` |
| Worksheet 04 review | Text with emoji was rejected before the documented length | Length counted in UTF-16 units, not characters | `CreateTaskRequestTests.countsCharactersNotUtf16UnitsSoEmojiCountOnce`, `TaskTests.savesATitleOf150EmojiBecauseLengthsCountCharacters` |
| Worksheet 04 review | The forms stopped typing at 75 emoji, and sent a 2-emoji title the API rejects | Same, in the browser `maxlength` and the form validators | `validators.spec`, length specs of the task, project and comment forms |

## 4. Refactoring

The priority and status badges were repeated on the board, the task page and My Tasks. They were moved into two shared components (`frontend/src/app/shared/badges.ts`), in a separate commit (`782f19c`). Behaviour was preserved: the existing frontend tests, which check the badge texts, passed without changes.

## 5. Limitations and what wasn't checked

- Only one end-to-end test (create a task, [test-plan.md](test-plan.md) T12); other browser checks were manual. Only Chromium was used.
- HTTPS wasn't used: the application runs locally ([ADR 0006](decisions/0006-run-locally.md)).
- Load times (NFR-04) weren't measured.
- Public sign-up (FR-18) and password recovery (FR-19) aren't built; they are optional.
- All demo accounts manage a project, so none of them can be deleted (EC-06). A real deletion was shown with a throwaway account.
- Open review findings, not fixed: deleting an account at the same moment as creating a project could leave a project without a manager; a sign-in with an unknown email answers slightly faster; simultaneous failed sign-ins can be under-counted; an assignment made while the member is being removed could remain. They need two requests at the same moment, and changing them affects security handling.

## 6. Use of AI

| Request | Developer's decision | How it was verified |
|---|---|---|
| Compare the MVP with the sheet's scope and propose a plan (Exercise 1). The AI listed the differences and recommended keeping public sign-up as a *Must*. | Rejected: the sheet takes precedence, so sign-up became optional and demo accounts come from a seed. The access rules were confirmed with the instructor. | Traceability review of requirements, stories, data model and contract; demo accounts tested by `SignInTests.demoAccountsAcceptThePasswordListedInTheReadme`. |
| Propose the implementation choices (Exercise 2). The AI proposed Flyway, PostgreSQL installed locally, and the demo password in an environment variable. | Corrected: Liquibase instead of Flyway, PostgreSQL in Docker Compose, and the fake demo password written in the README. | `SchemaAndSeedTests`; restart check (no data lost or duplicated); README followed from a clean clone. |
| Implement sign-in with a limit on failed attempts (Exercise 3). The AI recommended also counting failures for emails without an account. | Rejected as more than needed: only existing accounts are counted. The AI's first version also had a bug its own tests missed (CSRF cookie path), found in the browser. | `SignInTests.doesNotCountFailuresForEmailsWithoutAnAccount`, `blocksSignInAfterFiveFailedAttemptsEvenWithTheRightPassword`; `SessionCookieTests` for the cookie fix. |
