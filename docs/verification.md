---
title: Verification
---

# Matriz: Verification

> Results of checking the MVP against [requirements.md](requirements.md) and [user-stories.md](user-stories.md) (Practical Sheet 03, Exercises 8 and 9, and Practical Worksheets 04 and 05). Final run on 2026-11-18, for release candidate 1.1.0-rc.1 (section 8), on Windows 11 with Docker (Rancher Desktop), JDK 21, Node.js 24 and Chromium.

## 1. Commands and results

| Command | Where | Result |
|---|---|---|
| `./mvnw verify` | `backend/` | 145 tests, 0 failures (PostgreSQL 18 in Testcontainers) |
| `./mvnw test -Dtest=CreateTaskRequestTests` | `backend/` | Unit tests of the task title rule (Worksheet 04, Exercise 2): 11 tests, 0 failures, under 1 second, no Spring or database. Each case is checked for creating and editing a task. Seven deliberate changes to the rule (limits 2, 4 and 151, no not-blank check, no trimming, UTF-16 counting, and a looser edit rule) each made tests fail |
| `npx ng test --watch=false` | `frontend/` | 96 tests, 0 failures |
| `npx ng build` | `frontend/` | Build complete, no warnings |
| `npm run lint` | `frontend/` | All files pass linting |
| `./mvnw compile spotbugs:check` | `backend/` | No findings (SpotBugs with FindSecBugs) |
| `npm audit`; OSV-Scanner | `frontend/`, `backend/` | Frontend: 24 high, all in development tools, through `picomatch`, which has no fixed version yet (quality-security-review.md, F12); `npm audit --omit=dev`: 0. Backend: no issues in 165 packages |
| gitleaks (Docker) | whole Git history | No secrets found in 61 commits (up to `022b9e1`) |
| `E2E_PASSWORD=… npm run e2e` | `frontend/` | 1 Gherkin scenario (playwright-bdd) passed 3 runs in a row, each on a new `db-e2e` database, with its own servers (ports 8081, 4201); the development database was unchanged. Checked that it fails when the task isn't in the expected column, and when the page is reloaded after creating the task; the screenshot and trace were saved |
| Access between demo accounts (`curl`) | running app | 14 × 404 for a non-member, 4 × 403 for a member who isn't the PM, data unchanged ([quality-security-review.md](quality-security-review.md), section 3) |
| README steps from a clean clone | temporary folder | Passed: schema and demo data created, health `db: UP`, Alice signs in with the README password, app and `/api` proxy work; 82 backend and 79 frontend tests passed at commit `ff93f3d` (before the review fixes, which added 6 tests) |
| Worksheet 04 test commands from a clean clone at commit `2b44bce` | temporary folder | Passed: `./mvnw verify` 141 tests and `spotbugs:check`; `npm ci` (0 vulnerabilities), `ng test` 96 tests, `ng build`, `ng lint`; `npm run e2e` 1 scenario, on the clone's own `db-e2e` |
| Contract against code | `docs/openapi.yaml`, controllers | 25 operations (version 1.0.0); all implemented except `POST /users` (optional sign-up, FR-18) |

## 2. Cases

**A** = automated test (class or file, then test). **M** = manual check in the browser, done with each increment (details in [implementation-plan.md](implementation-plan.md)).

| # | Case | Expected | Observed | Evidence |
|---|---|---|---|---|
| 01 | Access to the application | Valid credentials sign in; wrong ones and requests without a session are refused | As expected. Wrong email and wrong password give the same message; the 6th failed attempt gets 429 with `Retry-After`; no session gives 401; an expired session sends the user to sign-in with a message | **A** `SignInTests` (17), `SessionCookieTests`; `sign-in.spec`, `auth.guards.spec`, `session-expired.interceptor.spec`. **M** Increments 2 and 3 |
| 02 | Project management | Create, search, edit and delete follow the permissions | As expected. Only members see a project; only the PM edits, deletes and manages members; search ignores case | **A** `ProjectTests` (13), `MemberTests` (12); `projects-page.spec`, `project-form-dialog.spec`, `project-detail.spec`, `members-dialog.spec`. **M** Increment 3 |
| 03 | Task persistence | A task appears without reloading and is still there after restarting the backend | As expected | **A** `TaskTests.createsATaskAsToDoWithMediumPriorityByDefault` (reads back from the database); `task-board.spec` (reload after create). **M** Increment 4, backend restarted |
| 04 | Data validation | Empty fields, fields made only of spaces and invalid values are rejected | As expected, with one error per field and nothing saved | **A** `TaskTests.rejectsATitleMadeOnlyOfSpacesAndSavesNothing`, `TaskTests.rejectsOtherInvalidFields`, `ProjectTests.rejectsInvalidNamesAndDescriptions`, `CommentTests.rejectsEmptyBlankAndTooLongComments`, `SignInTests.reportsInvalidFields`, `SchemaAndSeedTests.rejectsATitleMadeOnlyOfSpaces`; form specs. **M** Increments 3, 4 and 6 |
| 05 | Status and priority | Editing updates the task, the filters and the counts consistently | As expected. Late is cleared when a task is done; counts follow status changes | **A** `TaskTests.changesTheStatusInAnyDirection`, `filtersByStatusAndPriority`, `marksLateTasksUsingTodaysDate`, `projectCountsFollowStatusChanges`; `task-detail.spec`, `task-board.spec`, `dashboard.spec`. **M** Increments 4 and 7 |
| 06 | Assignees | Assigning and removing work; the same assignment isn't duplicated | As expected. Only project members can be assigned | **A** `AssigneeTests` (10), `SchemaAndSeedTests.rejectsADuplicateAssignment`; `assignees-panel.spec`, `my-tasks-page.spec`. **M** Increment 5 |
| 07 | Comments | The content, the signed-in author and the date are saved and shown | As expected. An author or date sent in the request is ignored | **A** `CommentTests` (6), including `ignoresAnAuthorOrDateSentInTheRequest`; `comments-panel.spec`. **M** Increment 6, backend restarted |
| 08 | Authorisation in the API | Another user can't see or change resources, even by changing the IDs in the request | As expected: 404 `PROJECT_NOT_FOUND` or `TASK_NOT_FOUND`, data unchanged | **A** `ProjectTests.hidesProjectsFromNonMembersEvenWhenTheIdIsChanged`, `TaskTests.nonMembersCannotSeeOrChangeTasksEvenWhenTheIdsAreChanged`, `TaskTests.aTaskCanOnlyBeReachedThroughItsOwnProject`, `AssigneeTests.nonMembersCannotAssignOrUnassignEvenWhenTheIdsAreChanged`, `CommentTests.nonMembersCannotReadOrAddCommentsEvenWhenTheIdsAreChanged`, `SignInTests.rejectsASignInWithoutTheCsrfToken`. **M** Increments 3 to 5 (Carol opening Alice's URLs) |
| 09 | Deletion and errors | Deletion respects dependencies; API errors don't show success or lose form data | As expected. Deleting a project or task leaves no orphan rows; forms keep their values after an error | **A** `ProjectTests.deletingAProjectDeletesItsTasksMembershipsAndAssignments`, `TaskTests.onlyTheProjectManagerCanDeleteATask`, `AccountDeletionTests` (8); `project-form-dialog.spec`, `new-task-dialog.spec`, `task-detail.spec`, `comments-panel.spec` ("keeps …" tests). **M** Increments 3, 4 and 8 |

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
| Worksheet 04, Exercise 5 | Late tasks used the UTC date, not the server's local date | The shared clock added in Exercise 3 was in UTC | `ClockConfigTests.usesTheServersLocalZoneForToday` |
| Worksheet 04, Exercise 5 | A title, name or comment of only no-break spaces was saved | Java and JavaScript disagree on what a space is | `TextTests`, `TaskTests.treatsNoBreakSpacesAsSpacesAndSavesTheTitleTrimmed`, `validators.spec` |
| Worksheet 04, Exercise 3 | A new task's `createdAt` and `updatedAt` in the create response (`.273471400Z`) differed from the saved ones (`.273471Z`) | The system clock is finer than the microseconds PostgreSQL stores | `TaskTests.createsATaskAsToDoWithMediumPriorityByDefault` (the `GET` must equal the `POST` response). Fixed with one clock ticking in microseconds (`common/time/ClockConfig`) |
| Worksheet 04 review | A password of more than 32 emoji was rejected, although 64 characters are allowed | `@Size` counts UTF-16 units | `SignInTests.countsTheLengthOfTheEmailAndPasswordInCharacters`, `sign-in.spec` |
| Worksheet 04 review | The forms stopped typing at 75 emoji, and sent a 2-emoji title the API rejects | Same, in the browser `maxlength` and the form validators | `validators.spec`, length specs of the task, project and comment forms |

## 4. Refactoring

The priority and status badges were repeated on the board, the task page and My Tasks. They were moved into two shared components (`frontend/src/app/shared/badges.ts`), in a separate commit (`782f19c`). Behaviour was preserved: the existing frontend tests, which check the badge texts, passed without changes.

## 5. Limitations and what wasn't checked

- Only one end-to-end test (create a task, [test-plan.md](test-plan.md) T12); other browser checks were manual. Only Chromium was used.
- HTTPS wasn't used: the application runs locally ([ADR 0006](decisions/0006-run-locally.md)).
- Load times (NFR-04) weren't measured.
- Public sign-up (FR-18) and password recovery (FR-19) aren't built; they are optional.
- All demo accounts manage a project, so none of them can be deleted (EC-06). A real deletion was shown with a throwaway account.
- Practical Worksheet 04:
  - Only one end-to-end scenario, and only in Chromium. The step definitions aren't type-checked (`@types/node` isn't installed; Playwright runs them without checking types).
  - SonarQube and Semgrep weren't used; ESLint, SpotBugs, OSV-Scanner and gitleaks were. SpotBugs runs on demand, not in `./mvnw verify`.
  - The database checks on names, titles and comments (`BTRIM` in `001-schema.sql`) only trim ASCII spaces. The API rejects other blank text first (quality-security-review.md, F7).
  - The optional extension (tests and static analysis in a CI/CD workflow) wasn't done.
- Practical Worksheet 05:
  - The free Render backend sleeps after about 15 minutes without requests, and the next request takes about a minute. Online, the demo accounts use the public demo password, so anyone can change the demo data.
  - Open findings, all Low ([quality-security-review.md](quality-security-review.md), section 7):
    - the failed sign-in counter has the F9 pattern (F10);
    - behind nginx and Render, `Location` headers point to the backend's own address (F11);
    - `npm audit` reports 24 high advisories in development tools, with no fixed version yet (F12).
  - Not done:
    - the optional challenges: A (dependency or image analysis in the pipeline), B (structured logs) and C (publishing images to the GitHub Container Registry). Images aren't published.
    - no tag or GitHub release was created for 1.1.0-rc.1.
  - Image and action versions are pinned by tag, not by digest or commit SHA.
- Open review findings, not fixed: deleting an account at the same moment as creating a project could leave a project without a manager; a sign-in with an unknown email answers slightly faster; simultaneous failed sign-ins can be under-counted; an assignment made while the member is being removed could remain. They need two requests at the same moment, and changing them affects security handling.

## 6. Use of AI

| Request | Developer's decision | How it was verified |
|---|---|---|
| Compare the MVP with the sheet's scope and propose a plan (Exercise 1). The AI listed the differences and recommended keeping public sign-up as a *Must*. | Rejected: the sheet takes precedence, so sign-up became optional and demo accounts come from a seed. The access rules were confirmed with the instructor. | Traceability review of requirements, stories, data model and contract; demo accounts tested by `SignInTests.demoAccountsAcceptThePasswordListedInTheReadme`. |
| Propose the implementation choices (Exercise 2). The AI proposed Flyway, PostgreSQL installed locally, and the demo password in an environment variable. | Corrected: Liquibase instead of Flyway, PostgreSQL in Docker Compose, and the fake demo password written in the README. | `SchemaAndSeedTests`; restart check (no data lost or duplicated); README followed from a clean clone. |
| Implement sign-in with a limit on failed attempts (Exercise 3). The AI recommended also counting failures for emails without an account. | Rejected as more than needed: only existing accounts are counted. The AI's first version also had a bug its own tests missed (CSRF cookie path), found in the browser. | `SignInTests.doesNotCountFailuresForEmailsWithoutAnAccount`, `blocksSignInAfterFiveFailedAttemptsEvenWithTheRightPassword`; `SessionCookieTests` for the cookie fix. |
| Practical Worksheet 04, Exercise 1: draft the test plan. | Reviewed against the user stories, the contract and the code; seven corrections, including a coverage claim that wasn't true. | [test-plan.md](test-plan.md), section 4. |
| Worksheet 04, Exercises 2 to 4: write and strengthen the tests. | Accepted after checking that each new test fails when the behaviour breaks; the end-to-end test was moved to its own database and to Gherkin at the developer's request. | Deliberate changes to the rule, the security rules and the page, each caught (sections 1 and 3). |
| Worksheet 04, Exercise 5: two AI code reviews and the security review. | Every finding checked by hand before deciding; the developer chose which to fix. One finding (F6) was a regression caused by an earlier AI change. | [quality-security-review.md](quality-security-review.md): evidence, decisions and regression tests. |
| Worksheet 05, Exercise 1: analyse the project and plan the delivery with Docker, Compose and CI. | The AI recommended renaming `docker-compose.yml` to `compose.yaml`, as the sheet does. The developer kept the existing name, and chose the full stack by default, CI with lint, end-to-end test and SpotBugs, and version 1.1.0-rc.1. | ADR 0007 and increments 10 to 14 |
| Exercises 2 to 4: write the Dockerfiles, Compose services and CI workflow. | Accepted after running them. The AI's first versions had problems it only found by running them: `npm ci` failed in the image (newer npm), and `mvnw` wasn't executable on Linux (first CI run). Its revert of the deliberate failure used an invalid `git revert -q`, then amended the wrong commit. The push was refused, so nothing wrong was published, and the developer approved resetting the local branch. | Builds, `docker run`, Compose checks in the browser, CI runs (section 8) |
| Online deployment (ADR 0008). The developer asked to deploy, and the AI proposed options for Render and Neon. | The developer chose Neon, a static site with an `/api` rewrite, the public demo password, and deploying after CI. The Neon CLI wasn't used: its install was refused by the agent's permission check, and the AI recommended skipping it. The developer pasted the database connection string into the chat; the AI used it only through environment variables, and asked for the password to be reset. | Live-site checks (increment 15); gitleaks; the old password isn't in the repository |
| Find why CI failed without code changes. | The developer read the CI log, which the AI can't open without a sign-in. The AI found the cause in the code (F9) and proposed a fix, which the developer approved. Its first repeatable test passed for the wrong reason (it hit the one-project-manager rule) and was corrected. | The new tests fail before the fix and pass after it; the race tests passed 10 runs in a row |
| Exercise 6: an independent AI review of the documentation, Docker setup, pipeline and test results. The reviewer was given the files, not the earlier conclusions. | 0 High, 3 Medium and 6 Low findings, each checked by hand. The developer chose to fix the documentation findings and record M2 and M3 as open (F11, F10). One suggested fix was wrong: using the project manager role in a test, for the reason above. | Section 8 |

## 7. Delivery (Practical Worksheet 04)

- **Branch and commit:** `main`, code as of commit `2b44bce`. Later commits only change documentation.
- **Not run or not checked, and still open:** section 5 above, and [quality-security-review.md](quality-security-review.md), section 6.

## 8. Release candidate 1.1.0-rc.1 (Practical Worksheet 05)

Delivery with Docker, Docker Compose, GitHub Actions and online hosting ([ADR 0007](decisions/0007-run-with-docker-compose-and-check-with-github-actions.md), [ADR 0008](decisions/0008-host-on-render-and-neon.md)). The details of each step are in [implementation-plan.md](implementation-plan.md), increments 10 to 15. The changes are in [CHANGELOG.md](../CHANGELOG.md) and the [release notes](releases/1.1.0-rc.1.md).

### Requirements of the worksheet

| Exercise | Result | Evidence |
|---|---|---|
| 1. Analysis and plan | Done | ADR 0007; increments 10 to 14 |
| 2. Docker images | Done: multi-stage, non-root, no credentials | Increment 10 |
| 3. Docker Compose | Done: `db`, `backend` and `frontend` with healthchecks; data kept after `down` and `up` | Increment 11 |
| 4. CI | Done: passes on `main`, fails when a test fails, and builds the images only after the checks | Increment 12; the runs below |
| 5. Documentation and version | Done: README, architecture with Mermaid diagrams, OpenAPI servers, CHANGELOG, release notes, version 1.1.0-rc.1 | Increment 13 |
| 6. Final validation | Done: this section | Increment 14 |
| Online deployment (module 5 note) | Done: https://matriz-c1ft.onrender.com | Increment 15 |
| Challenge A: dependency or image analysis in the pipeline | Not done. `npm audit`, OSV-Scanner and gitleaks are run by hand | — |

### CI runs

| Run | Commit | Result |
|---|---|---|
| [43093051120](https://github.com/barbarafoliv/matriz-project-management/actions/runs/43093051120) | `9519d01` | Failed: `./mvnw` not executable (fixed in `de6c1b5`) |
| [43094065698](https://github.com/barbarafoliv/matriz-project-management/actions/runs/43094065698) | `de6c1b5` | Passed |
| [43094808256](https://github.com/barbarafoliv/matriz-project-management/actions/runs/43094808256) | `0ed961e` | Failed as intended: a deliberately broken test; the Docker images were skipped |
| [44272020105](https://github.com/barbarafoliv/matriz-project-management/actions/runs/44272020105) | `4706e5a` | Passed again after the revert |
| [44281102452](https://github.com/barbarafoliv/matriz-project-management/actions/runs/44281102452) and [44285097799](https://github.com/barbarafoliv/matriz-project-management/actions/runs/44285097799) | `100da23`, `af0b56d` | Failed: a race when adding a member (F9), on commits that didn't change the backend |
| [44287599223](https://github.com/barbarafoliv/matriz-project-management/actions/runs/44287599223) | `d104a43` | Passed with the F9 fix. Render then deployed the backend: Live at 10:48, after CI finished at 10:45:55 |
| [44289207248](https://github.com/barbarafoliv/matriz-project-management/actions/runs/44289207248) | `022b9e1` | Passed |

### Final validation (2026-11-18)

- **The README from a clean clone of `022b9e1`**, with its own containers and volume, and `.env` copied from `.env.example`:
  - `docker compose up -d --build --wait`: all three services healthy, with no `WARN` or `ERROR` lines in the logs.
  - Through http://localhost:4200: health `db: UP`; 401 without a session; Alice signs in with the README password; the cookies aren't `Secure` locally.
  - Through the API: create a task, change its status, comment, and assign a member. Assigning the same member again gets 409.
  - The data is still there after `docker compose down` and `up -d`.
- **The README's test commands, in the same clone:**
  - `./mvnw verify`: 145 tests, 0 failures. `spotbugs:check`: no findings.
  - `npm ci` with npm 11.6.2; `npm run lint`: all files pass. `ng test`: 96 tests in 19 files, 0 failures. `ng build`: complete.
  - `npm run e2e`: 1 scenario passed.
- **Online:** health through the frontend, sign-in, a task created and moved to In Progress, `Secure` cookies, and the task still there after a backend redeploy (increment 15).
- **Credentials:** gitleaks found no leaks in 61 commits, and `.env` isn't tracked. The Neon password that was pasted into the AI chat was reset, and isn't in the repository.

### Problems found and fixed

| Problem | Fix |
|---|---|
| `npm ci` failed in the image and in CI, because newer npm rejects the lock file | Install npm 11.6.2, as declared in `packageManager` |
| `./mvnw` failed with "permission denied" on Linux and macOS | File mode `100755` (`de6c1b5`) |
| The revert of the deliberate failure used `git revert -q`, which failed, and the next `--amend` rewrote the broken commit | The push was refused. The local branch was reset to `origin/main`, with approval, and the revert was redone (`715fddd`) |
| The Render name `matriz-api` was taken, so the `/api` rewrite pointed to another service | Rewrite to `matriz-api-8rzw` (`100da23`) |
| Two simultaneous requests to add or assign the same member both answered 201 (F9) | `Persistable` entities (`d104a43`) |
| `docker compose up -d` reused old local images on a machine that already had them | The README says to use `--build` after a code change or a `git pull` |
| An `npm start` already running held port 4200 on `127.0.0.1` | Stopped. The README explains that only one of them can run at a time |
| Review findings in the documentation: the F9 wording, matching `.env` passwords, the Neon URL in `render.yaml`, ADR 0008's deploy wording, and the release notes' link | Corrected |

### Not done, and still open

Section 5 above, and [quality-security-review.md](quality-security-review.md), section 7 (F10 to F12).
