# Project instructions

## Context

Matriz is a project and task management application.

Before starting a task, read the documents relevant to it:
- [docs/requirements.md](docs/requirements.md): goals, user types, requirements (FR, NFR), MVP scope and out-of-scope features.
- [docs/user-stories.md](docs/user-stories.md): MVP user stories (US), acceptance criteria and exceptional cases (EC).
- [docs/constraints.md](docs/constraints.md): constraints, assumptions, security and privacy rules, pending technical decisions.
- [README.md](README.md): project overview, current status, and build and test commands (once defined).

These documents are the source of truth. The `course-notes/` folder is not product documentation.

## Conventions

- Follow the existing organisation and patterns of the code.
- Keep business logic out of the controllers and out of the user interface.
- Don't introduce dependencies without justifying the need, and get approval first.
- Never write passwords, keys, tokens or other secrets into code, documentation, commits or output. Use only fake personal data.

## Process

- Analyse the relevant files before changing code.
- Only implement what the task asks for and the documentation supports, and refer to the related IDs (e.g. FR-04, US-04). Don't invent features, rules or decisions. Suggest them instead.
- Make ambiguities and assumptions explicit. If the documentation is unclear or contradictory, ask before going on.
- Don't make the pending technical decisions in `docs/constraints.md` on your own. Propose options and wait for approval.
- Before a significant change (several files, a new dependency, a structural change or a change to `docs/`), present a short plan and wait for approval.
- Limit changes to the goal of the task. Report problems found outside it, but don't fix them.
- Ask for approval before destructive or sensitive actions: deleting or overwriting files or data, rewriting version-control history, pushing or publishing, or changing security or personal data handling.

## Validation

- Use the build and test commands in the README. If they aren't defined yet, say so.
- Add or update tests for any behaviour you change, based on the acceptance criteria where they exist.
- Report the commands you ran and their results, the files you changed, and any assumptions you made.
- If you can't validate a change, explain what prevented it.
