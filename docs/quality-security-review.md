---
title: Quality and security review
---

# Matriz: Quality and security review

> Code Review and Security Review (Practical Worksheet 04, Exercise 5), on 2026-11-04. Rules checked against [constraints.md, section 3](constraints.md#3-security-and-privacy), EC-04 and [openapi.yaml](openapi.yaml).

## 1. Scope and checks

| Check | How | Done |
|---|---|---|
| Code Review | AI review of `git diff 466333c^..HEAD -- backend frontend` (account deletion, dashboard, badge refactor, earlier review fixes, Worksheet 04 tests), focused on bugs, validation, error handling, access control and missing tests. Each finding was checked by hand. | Yes |
| Access between accounts | `curl` against the running app with the demo accounts, see section 3 | Yes |
| Static analysis | Frontend: ESLint with angular-eslint (`npm run lint`). Backend: SpotBugs 4.10 with FindSecBugs 1.14 (`./mvnw compile spotbugs:check`), also once with threshold Low | Yes |
| Dependencies | Frontend: `npm audit`, and OSV-Scanner on `package-lock.json` (649 packages). Backend: OSV-Scanner on a CycloneDX list of all dependencies, test ones included (165 packages). The scanner was first checked with a known vulnerable package (log4j-core 2.14.1, found) | Yes |

## 2. Findings

**Confirmed** = traced in the code or reproduced. **Suspected** = plausible, not shown.

| # | Where | Finding | Evidence | Impact | Severity | Status | Proposed fix and regression test |
|---|---|---|---|---|---|---|---|
| F1 | `backend/.../accounts/AccountDeletionService.java:58` | Sessions are deleted before the account deletion is committed | Spring Session's `JdbcHttpSessionConfiguration` 4.1.1 runs session operations with `REQUIRES_NEW` (checked in the bytecode), so they commit on their own, before the outer transaction deletes the user | If the user deletion then fails, the user is signed out everywhere but the account is kept, and the API answers 500. Safe: no access is kept, nothing leaks | Low | Confirmed: reproduced by rolling back the deletion (section 4); no real constraint makes the deletion fail today | Delete the sessions after the commit. Test: make the user deletion fail and check the account and its sessions are kept |
| F2 | `backend/.../tasks/UpdateTaskRequest.java:17`, `projects/ProjectRequest.java` | Missing test: on edit, the description length is checked after trimming | Only create is tested (`TaskTests.checksTheDescriptionLengthAfterTrimming`, `ProjectTests`) | A future change to edit could reject valid text without a failing test | Low | Confirmed (test gap) | `PUT` a task with 2000 characters plus spaces, and a project with 1000; expect 200 and the trimmed value |
| F3 | `backend/.../common/web/TrimmedLength.java:51` | Length is counted in UTF-16 units, not characters | `String.length()` counts an emoji as 2; `VARCHAR(n)` and OpenAPI `maxLength` count characters | Text with emoji is rejected before the documented maximum. Nothing too long gets through. The forms counted the same way (fixed too, see section 4) | Low | Confirmed | Count code points. Unit test: a 150-emoji title is accepted, 151 rejected |
| F4 | `frontend/package-lock.json` | 31 moderate advisories in development tools (`glob`, `minimatch` through the Angular CLI and ESLint) | `npm audit`; `npm audit --omit=dev`: 0. Before this worksheet: 16, all in development tools | None in the delivered app; only build tools on the developer's machine | Low | Confirmed | Update when the Angular and ESLint versions with fixes are released. `npm audit fix --force` would install breaking versions |
| F5 | `backend/.../tasks/TaskView.java`, `MyTask.java` | SpotBugs `EI_EXPOSE_REP` (4, Medium): the views keep the list of assignees they are given, so it can be changed later | Records store the `List` as it is | None found today: views are built from new lists and not changed. A later change could alter a view by mistake | Low | Confirmed (code) | Copy the list in the record (`List.copyOf`). Test: changing the original list or the view's list has no effect |

### ESLint results

`npm run lint` reports 6 errors. None is a defect in the app.

| Where | Rule | Assessment |
|---|---|---|
| `dashboard.ts:52`, `projects-page.html:53` | `<a>` should have content | False positive: the project card component fills the link and gives it an `aria-label` |
| `members-dialog.ts:62` | Parsing error | The linter can't read the escaped `\\'` in an inline template; the Angular compiler can, and the dialog is tested |
| `project-card.ts:12` | Component selector prefix | Intentional `a[app-project-card]` attribute selector, so the card is a real link |
| `modal.ts:11` (2) | Click without a key event, not focusable | The overlay click is a mouse shortcut; the dialog closes with Escape and the ✕ button |

### SpotBugs results (threshold Low)

Besides F5, all Low and none a defect:

| Finding | Assessment |
|---|---|
| `CRLF_INJECTION_LOGS` in `ApiExceptionHandler.java:61`, which logs the request method and URI | False positive. `getRequestURI()` isn't decoded, so a line break stays `%0a`; requests with encoded line breaks are rejected before the app (400, Spring's default error body, checked with `curl`), and the HTTP parser doesn't accept raw ones |
| `SPRING_ENDPOINT` (22) | Information: lists the endpoints to review. They were reviewed in section 3 |
| `THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION` in `SecurityConfig` | Style: `throws Exception` comes from Spring Security's `HttpSecurity.build()` |

Dependencies: OSV-Scanner found no known vulnerabilities in the backend or frontend dependencies.

## 3. Access between accounts (IDOR / Broken Access Control)

Alice is the project manager of "Website redesign" (project 1, Bob is a member). Carol isn't a member. Carol used Alice's real project and task IDs; Bob tried the PM-only actions.

| Request by | Requests | Result |
|---|---|---|
| Carol | `GET`, `PUT`, `DELETE` project; `GET`, `POST` members (adding herself); `GET`, `POST` tasks; `GET`, `PUT`, `DELETE` task; `PUT` status; `POST` assignee (herself); `GET`, `POST` comments | 404 `PROJECT_NOT_FOUND` for all 14 |
| Carol | `GET /projects`, `GET /projects?q=web`, `GET /users/me/tasks` | Only her own project; no task of Alice's project |
| Bob | Edit and delete the project, add a member, delete a task | 403 `NOT_PROJECT_MANAGER` for all 4 |
| No session | `GET` a task | 401 `UNAUTHENTICATED` |
| Alice, without the CSRF token | `PUT` status | 403 `CSRF_TOKEN_INVALID` |

Afterwards, the project, its members and the task were unchanged (`updated_at` equal to `created_at`). **No vulnerability found.** The same rules are covered by automated tests (verification.md, case 08).

## 4. Decisions

The developer reviewed the findings and asked for all of them to be fixed. Each regression test failed before its fix and passes after it.

| Finding | Decision | Fix | Regression test |
|---|---|---|---|
| F1 | Fixed | Sessions are deleted after the deletion is committed (`TransactionSynchronization.afterCommit`) | `AccountDeletionTests.keepsTheSessionsWhenTheDeletionIsNotCommitted`. It reproduced the problem: with the old code, a rolled-back deletion kept the account but removed its session |
| F2 | Fixed (tests only) | — | `TaskTests` and `ProjectTests.checksTheDescriptionLengthAfterTrimming` now also edit (`PUT`) |
| F3 | Fixed | `TrimmedLength` counts code points | `CreateTaskRequestTests.countsCharactersNotUtf16UnitsSoEmojiCountOnce`, `TaskTests.savesATitleOf150EmojiBecauseLengthsCountCharacters` (saved in PostgreSQL) |
| F4 | Fixed | npm `overrides` in `frontend/package.json` force the patched `glob` 14.0.3 and `minimatch` 11.0.4, without changing the Angular or ESLint versions. Remove them once those tools ship the fixes | `npm audit`: 0. Build, unit tests, lint (60 files linted, a planted error is still caught), `ng generate` and E2E all pass |
| F5 | Fixed | `List.copyOf` in `TaskView` and `MyTask`; `spotbugs:check` passes | `TaskViewTests.theAssigneesCannotBeChangedAfterTheViewIsBuilt` |
| Email and password lengths | Fixed | Sign-in email and password, and member email: `@CodePointLength` (Hibernate Validator) instead of `@Size`; the sign-in form counts characters too (the password isn't trimmed). The API error codes (`TOO_LONG`, `TOO_SHORT`) now count the same way as the constraint that failed. A 64-character password of 256 bytes is checked normally, although BCrypt only uses the first 72 bytes (no error) | `SignInTests.countsTheLengthOfTheEmailAndPasswordInCharacters`, `MemberTests.rejectsAnEmailLongerThan254Characters`, `TaskTests.savesATitleOf150EmojiBecauseLengthsCountCharacters` (error codes), `sign-in.spec`, `validators.spec` |
| Frontend lengths | Fixed | The forms count characters like the API (`shared/validators.ts`): the native `maxlength` was replaced by a validator with a message, so typing isn't cut at 75 emoji, and a 2-emoji title isn't sent | `validators.spec`, and the length specs of the task, project and comment forms |
| ESLint | Fixed; `npm run lint` passes | Members dialog: message moved into the component (same text). Rules set for the intentional patterns: attribute selectors with the `app` prefix, and `app-project-card` counted as link content. Modal overlay: rule turned off on that line, with the reason | `members-dialog.spec` checks the full message |

After the fixes, the access check in section 3 was run again on the restarted backend, with the same results. Note for manual checks on Windows: emoji passed to `curl` as a command-line argument arrive altered; send the body from a file (`--data-binary @body.json`).

## 5. Not checked

- Simultaneous requests (open findings in [verification.md](verification.md), section 5).
