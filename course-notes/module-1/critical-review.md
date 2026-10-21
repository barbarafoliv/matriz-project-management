# Critical Review of the Matriz Documentation

> The AI reviewed the documentation and identified the findings below. **Every finding was reviewed by a human, who made the decision on each one.** Only these decisions were applied to the documents.

**Decisions:** **Accept** = apply the fix · **Modify** = apply a different fix · **Reject** = leave as it is.

## 1. Ambiguous or contradictory requirements

| # | Finding | Decision |
|---|---|---|
| 1 | Team members only "update the status" of tasks (requirements §2), but FR-04 lets any member create, edit and delete tasks. | **Accept:** members create and edit tasks, and only the project manager deletes them. |
| 2 | Goal 2 says every task has an owner and a due date, but both fields are optional. | **Modify:** Goal 2 is now "Everyone knows which tasks are theirs and when they're due." |

## 2. Unjustified features

| # | Finding | Decision |
|---|---|---|
| 3 | FR-13 (priority matrix) is justified only by the product name. | **Modify:** it groups tasks by urgency (due date) and importance (priority), to help decide what to work on first. |
| 4 | The task priority field is in the MVP, but no MVP feature uses it. | **Modify:** keep the field, define its values (Low, Medium, High) and show it on the board card. |

## 3. Assumptions presented as facts

| # | Finding | Decision |
|---|---|---|
| 5 | The problem statement was never confirmed with users. | **Modify:** added to the assumptions in `constraints.md`. |
| 6 | "The team is small and the timeline is short" is listed as a constraint without a source. | **Reject:** it's a real, known constraint, so it stays. |

## 4. Acceptance criteria that can't be verified

| # | Finding | Decision |
|---|---|---|
| 7 | "All members see the changes" doesn't say when (US-02, US-04, US-05). | **Accept:** changed to "when they next open the project". |
| 8 | "The change is saved" (US-05) can't be observed. | **Accept:** changed to "the task shows In Progress". |

## 5. Security and privacy risks

| # | Finding | Decision |
|---|---|---|
| 9 | GDPR is required, but users can't delete their account or personal data. | **Accept:** added as FR-14 and US-09. |
| 10 | There are no password rules and no limit on failed sign-in attempts. | **Modify:** one general requirement added to NFR-02, with exact values left to the design. |

## 6. Inconsistencies between documents

| # | Finding | Decision |
|---|---|---|
| 11 | `constraints.md` leaves NFR-01 to NFR-03 out of the MVP, but `requirements.md` includes them. | **Accept:** `constraints.md` now includes them. |
| 12 | `../../AGENTS.md` mentions build and test commands in the README, which don't exist yet. | **Accept:** added "(once defined)". |
