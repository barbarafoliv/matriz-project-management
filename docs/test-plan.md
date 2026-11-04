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
| E2E | Gherkin scenarios run by Playwright (Chromium) with playwright-bdd, in its own environment: an empty `db-e2e` database with the demo data, and its own backend and frontend | `npm run e2e` (see README) |

## 2. Scenarios

**E** = essential scenario chosen for this worksheet.

| # | Scenario | Input | Expected result | Level | Test |
|---|---|---|---|---|---|
| T01 **E** | Valid title (create and edit) | `"Write the copy"` | Accepted | Unit | `CreateTaskRequestTests.acceptsAValidTitle` |
| T02 **E** | Title at the limits | 3 and 150 characters, also with spaces around | Accepted | Unit | `acceptsTitlesAtTheLimits`, `ignoresLeadingAndTrailingSpacesWhenCountingTheLength` |
| T03 **E** | Title just outside the limits | 2 and 151 characters, `"  ab  "` | Rejected: "Title must be between 3 and 150 characters." | Unit | `rejectsTitlesJustOutsideTheLimits`, `rejectsATitleThatIsTooShortOnceTrimmed` |
| T03b | Emoji in the title | 150 and 151 emoji | 150 accepted and saved, 151 rejected (lengths count characters) | Unit, Integration | `countsCharactersNotUtf16UnitsSoEmojiCountOnce`, `TaskTests.savesATitleOf150EmojiBecauseLengthsCountCharacters` |
| T04 **E** | Empty, blank or missing title | `""`, `"     "`, no title | Rejected: "Title must not be empty." | Unit | `rejectsAnEmptyTitleOrOneMadeOnlyOfSpaces`, `rejectsAMissingTitle` |
| T05 **E** | Invalid title through the API | `"     "` | 400 `VALIDATION_FAILED`, field `title`, nothing saved | Integration | `TaskTests.rejectsATitleMadeOnlyOfSpacesAndSavesNothing`, `rejectsOtherInvalidFields` |
| T06 **E** | Create and read a task | Member creates `"  Write the copy  "` | 201, `Location` header, title trimmed, To Do, Medium; found in the database and through `GET` by another member | Integration | `TaskTests.createsATaskAsToDoWithMediumPriorityByDefault` |
| T07 **E** | Task that doesn't exist | `GET` and `PUT` on an unknown task ID in my project | 404 `TASK_NOT_FOUND`, nothing created | Integration | `TaskTests.reportsATaskThatDoesNotExistAsNotFound` |
| T08 **E** | Non-member access | A non-member lists, opens, creates, edits, changes status and deletes, using real IDs | 404 `PROJECT_NOT_FOUND` for each, task unchanged | Integration | `TaskTests.nonMembersCannotSeeOrChangeTasksEvenWhenTheIdsAreChanged` |
| T09 | Task reached through another project | Task ID of project A under project B | 404 `TASK_NOT_FOUND` | Integration | `TaskTests.aTaskCanOnlyBeReachedThroughItsOwnProject` |
| T10 | Only the PM deletes a task | A member, then the PM, deletes a task | Member: 403 `NOT_PROJECT_MANAGER`; PM: 204 | Integration | `TaskTests.onlyTheProjectManagerCanDeleteATask` |
| T10b | A member who isn't the PM edits a task | Ben (member) changes the title, description, due date and priority | 200 with the new values; another member sees the new title | Integration | `TaskTests.anyMemberCanEditATaskAndNullClearsOptionalFields` |
| T11 | No session | Every task request (list, open, create, edit, change status, delete) without a session, with a valid CSRF token | 401 `UNAUTHENTICATED` for each, nothing created or changed | Integration | `TaskTests.requestsWithoutASessionAreRefusedAndChangeNothing`; also `SignInTests.requiresASessionForTheSignedInUser` (`/users/me`) |
| T12 **E** | Create a task in the browser | Alice signs in, opens a new project, creates a task | The task is shown in the To Do column without reloading | E2E | `frontend/e2e/features/create-task.feature`, scenario "A new task appears in the To Do column without reloading the page" |
| T13 | Manual cross-account check | Carol uses the IDs of Alice's project and tasks | Not shown, not changed | Manual | [quality-security-review.md](quality-security-review.md) |

## 3. Decisions

- **Non-members get 404, not 403.** EC-04 says the project is shown "as if it didn't exist". A 403 would confirm that it exists.
- **The unit test checks the request rule, not the validator alone.** It validates `CreateTaskRequest` and `UpdateTaskRequest`, and requires both to give the same errors, so a change to either one's annotations (limits, messages) is caught. T01–T04 apply to creating and editing.
- **The rule was changed only where it didn't match the contract.** Counting emoji as two characters (T03b) went against `maxLength`; the test was written first, failed, and then the rule was fixed. No test was changed to fit the code.
- **The limits are tested on both sides** (2/3 and 150/151), and after trimming, because the contract counts the trimmed length.
- **Existing integration tests are reused.** They already cover T05, T06 and T08; only T07 was missing.
- **E2E scenarios are written in Gherkin**, in the same Given / When / Then form as the acceptance criteria in user-stories.md, so they can be read and checked against them.
- **The E2E test has its own environment.** `npm run e2e` recreates an empty database (`db-e2e`), and Playwright starts a backend that loads the demo data into it, and a frontend. The development data can't be changed by the tests, and each run starts from the same data.
- **The E2E test creates and deletes its own project**, so it doesn't depend on the other demo projects, and can be run again in the same environment.
- **Permissions are tested both ways:** what a member may do (T06, T10b) and what a non-member or a member who isn't the PM may not (T08, T10).
- **Not in this plan:** status changes, filters, assignees and comments. They are covered in [verification.md](verification.md).

## 4. Review of the AI draft

The scenarios were drafted with an AI assistant and checked by the developer against the user stories, the contract and the code.

| AI suggestion | What the review found | Decision |
|---|---|---|
| T11 covered by `SessionCookieTests` for "any task request" | No test sent a task request without a session; only `/users/me` was tested | Test added for every task endpoint. Checked that it fails when anonymous `GET` requests are allowed |
| Title limits tested only with plain ASCII text | The review found that an emoji counted as two characters (quality-security-review.md, F3) | T03b added, rule fixed |
| E2E test on the development database, with demo data | The worksheet asks for an isolated test environment | Separate `db-e2e` database and servers |
| E2E test as a plain Playwright spec | The developer asked for Gherkin, like the acceptance criteria | playwright-bdd feature file (T12) |
| Permission rows only for forbidden actions | A member's allowed edit (FR-04) wasn't shown | T10b added |
