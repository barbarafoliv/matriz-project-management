---
title: Test plan
---

# Matriz: Test plan

> Tasks and their permissions (FR-04, US-04, EC-03, EC-04). Expected results come from [user-stories.md](user-stories.md) and [openapi.yaml](openapi.yaml), not from the current code. Practical Worksheet 04.

## 1. Test levels

| Level | Tool | Command |
|---|---|---|
| Unit | JUnit 5 (backend), Vitest (frontend) | `./mvnw verify`, `npx ng test --watch=false` |
| Integration | Spring Boot Test, MockMvc, PostgreSQL in Testcontainers | `./mvnw verify` |
| E2E | Playwright (Chromium), against the running app | `npm run e2e` (see README) |

## 2. Scenarios

**E** = essential scenario chosen for this worksheet.

| # | Scenario | Input | Expected result | Level | Test |
|---|---|---|---|---|---|
| T01 **E** | Valid title | `"Write the copy"` | Accepted | Unit | `CreateTaskRequestTests.acceptsAValidTitle` |
| T02 **E** | Title at the limits | 3 and 150 characters, also with spaces around | Accepted | Unit | `acceptsTitlesAtTheLimits`, `ignoresLeadingAndTrailingSpacesWhenCountingTheLength` |
| T03 **E** | Title just outside the limits | 2 and 151 characters, `"  ab  "` | Rejected: "Title must be between 3 and 150 characters." | Unit | `rejectsTitlesJustOutsideTheLimits`, `rejectsATitleThatIsTooShortOnceTrimmed` |
| T04 **E** | Empty, blank or missing title | `""`, `"     "`, no title | Rejected: "Title must not be empty." | Unit | `rejectsAnEmptyTitleOrOneMadeOnlyOfSpaces`, `rejectsAMissingTitle` |
| T05 **E** | Invalid title through the API | `"     "` | 400 `VALIDATION_FAILED`, field `title`, nothing saved | Integration | `TaskTests.rejectsATitleMadeOnlyOfSpacesAndSavesNothing`, `rejectsOtherInvalidFields` |
| T06 **E** | Create and read a task | Member creates `"  Write the copy  "` | 201, `Location` header, title trimmed, To Do, Medium; found in the database and through `GET` by another member | Integration | `TaskTests.createsATaskAsToDoWithMediumPriorityByDefault` |
| T07 **E** | Task that doesn't exist | `GET` and `PUT` on an unknown task ID in my project | 404 `TASK_NOT_FOUND`, nothing created | Integration | `TaskTests.reportsATaskThatDoesNotExistAsNotFound` |
| T08 **E** | Non-member access | A non-member lists, opens, creates, edits, changes status and deletes, using real IDs | 404 `PROJECT_NOT_FOUND` for each, task unchanged | Integration | `TaskTests.nonMembersCannotSeeOrChangeTasksEvenWhenTheIdsAreChanged` |
| T09 | Task reached through another project | Task ID of project A under project B | 404 `TASK_NOT_FOUND` | Integration | `TaskTests.aTaskCanOnlyBeReachedThroughItsOwnProject` |
| T10 | Only the PM deletes a task | A member, then the PM, deletes a task | Member: 403 `NOT_PROJECT_MANAGER`; PM: 204 | Integration | `TaskTests.onlyTheProjectManagerCanDeleteATask` |
| T11 | No session | A request without a session | 401 `UNAUTHENTICATED` | Integration | `SignInTests.requiresASessionForTheSignedInUser` (only `/users/me`; task endpoints rely on the same global rule in `SecurityConfig`, not tested separately) |
| T12 **E** | Create a task in the browser | Alice signs in, opens a new project, creates a task | The task is shown in the To Do column without reloading | E2E | `frontend/e2e/create-task.spec.ts` |
| T13 | Manual cross-account check | Carol uses the IDs of Alice's project and tasks | Not shown, not changed | Manual | [quality-security-review.md](quality-security-review.md) |

## 3. Decisions

- **Non-members get 404, not 403.** EC-04 says the project is shown "as if it didn't exist". A 403 would confirm that it exists.
- **The unit test checks the request rule, not the validator alone.** It validates `CreateTaskRequest`, so a change to the annotations (limits, messages) is caught.
- **The limits are tested on both sides** (2/3 and 150/151), and after trimming, because the contract counts the trimmed length.
- **Existing integration tests are reused.** They already cover T05, T06 and T08; only T07 was missing.
- **The E2E test creates and deletes its own project**, so it doesn't depend on, or change, the demo data, and can be run again.
- **Not in this plan:** status changes, filters, assignees and comments. They are covered in [verification.md](verification.md).
