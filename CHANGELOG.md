# Changelog

All notable changes to Matriz. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

## [1.1.0-rc.1] - 2026-11-18

Release candidate. No features change. The REST API has the same endpoints and data (contract version 1.0.0); its rules for spaces and lengths are described more precisely. [Release notes](docs/releases/1.1.0-rc.1.md).

### Added

- Docker images for the backend and frontend, both running as non-root users. nginx serves the frontend and forwards `/api` to the backend ([ADR 0007](docs/decisions/0007-run-with-docker-compose-and-check-with-github-actions.md)).
- The whole application runs with Docker Compose on http://localhost:4200, with healthchecks.
- A GitHub Actions pipeline on every push and pull request. It runs the backend tests and SpotBugs, the frontend lint, tests and build, and the end-to-end test, and builds the Docker images only after they pass.
- Online deployment on Render and Neon (`render.yaml`, [ADR 0008](docs/decisions/0008-host-on-render-and-neon.md)), at https://matriz-c1ft.onrender.com.
- `COOKIES_SECURE` setting: the session and CSRF cookies are only sent over HTTPS (on online, off by default).
- End-to-end test in Gherkin (Playwright with playwright-bdd), with its own database.
- Static analysis and dependency checks: ESLint, SpotBugs with FindSecBugs, `npm audit`, OSV-Scanner and gitleaks.
- Test plan and quality and security review ([docs/test-plan.md](docs/test-plan.md), [docs/quality-security-review.md](docs/quality-security-review.md)).

### Changed

- `docker compose up -d` now starts the whole application. For development, start only the database with `docker compose up -d db`.
- Times are saved in microseconds, as PostgreSQL stores them.

### Fixed

- Lengths of names, titles, descriptions, comments, emails and passwords count characters, not UTF-16 units, in the API and the forms. An emoji counts once.
- Names, titles and comments made only of Unicode spaces (for example no-break spaces) are rejected, as in the forms.
- Deleting an account ends its sessions only after the deletion is saved.
- `backend/mvnw` is executable, so `./mvnw` works on Linux and macOS.

### Security

- Patched `glob` and `minimatch` versions are forced in the frontend's development tools (npm `overrides`).

## [1.0.0] - 2026-11-04

First version: the MVP, with all *Must* requirements (FR-01 to FR-10, FR-14 to FR-17).

### Added

- Sign-in and sign-out with server-side sessions. Sign-in is blocked for 15 minutes after 5 failed attempts, and sessions expire after 30 minutes. Demo accounts are created by a seed.
- Projects: create, list, search, edit and delete; members.
- Tasks: create, edit and delete; status; a board per project; filters by status and priority; late tasks highlighted.
- Assignees and "My tasks".
- Comments.
- Dashboard with the task counts per status.
- Deleting an account and its personal data.

[1.1.0-rc.1]: https://github.com/barbarafoliv/matriz-project-management/compare/v1.0.0...v1.1.0-rc.1
[1.0.0]: https://github.com/barbarafoliv/matriz-project-management/releases/tag/v1.0.0
