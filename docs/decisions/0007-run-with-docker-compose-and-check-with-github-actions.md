# 0007 - Run with Docker Compose and Check with GitHub Actions

## Status

Accepted

## Context

Until now, only PostgreSQL ran in Docker. The backend and frontend were started by hand, with JDK 21 and Node.js installed on the machine ([ADR 0006](0006-run-locally.md)), and the tests were only run locally. The delivery must show that Matriz can be built, tested and run the same way on any machine with Docker, and that every change is checked automatically.

## Decision

- **Containers:** the backend and the frontend each have a multi-stage Dockerfile.
  - The backend image is built with the Maven wrapper and runs on a Java 21 JRE, as a non-root user.
  - The frontend image is built with Node.js and served by nginx. nginx also forwards `/api` to the backend, so the browser sees one origin and the session cookie works as it does with `ng serve` ([ADR 0004](0004-use-email-password-with-server-side-sessions.md)).
  - No credentials are in the images. Settings come from environment variables (`.env`, see `.env.example`).
- **Docker Compose:** `docker-compose.yml` defines `db`, `backend` and `frontend`, with healthchecks, and `docker compose up -d` starts all three. Each service waits until the one it depends on is healthy. For development with `./mvnw` and `npm start`, only the database is started (`docker compose up -d db`). The `db-e2e` database stays behind the `e2e` profile.
- **Continuous integration:** a GitHub Actions workflow (`.github/workflows/ci.yml`) runs on every push and pull request, with read-only permissions. It runs:
  - backend: tests and SpotBugs;
  - frontend: ESLint, tests and build;
  - the end-to-end test.

  The Docker images are built only when all of these pass. Images aren't published.

## Alternatives considered

- **Backend serves the Angular files:** one image, but it mixes the client into the backend build, against the separation in [ADR 0003](0003-use-rest-between-clients-and-api.md).
- **Backend and frontend behind a Compose profile:** keeps `docker compose up -d` database-only, but every full-stack command would need the profile flag.
- **No CI, tests run by hand:** simpler, but nothing stops a change that breaks the tests from reaching `main`.

## Consequences

- The application runs with only Docker installed. JDK 21 and Node.js are still needed for development and for running the tests locally.
- Hosting stays local ([ADR 0006](0006-run-locally.md)). The CI workflow builds and tests, but doesn't deploy anything.
- `docker compose up -d` now starts the whole application on http://localhost:4200, the same address as `npm start`, so the two can't run at the same time. The development flow changes to `docker compose up -d db`.
- The CI run takes several minutes, mainly because of the end-to-end test and Testcontainers.
