# 0008 - Host on Render and Neon

## Status

Accepted. Supersedes [ADR 0006](0006-run-locally.md) for hosting; local development is unchanged.

## Context

Matriz ran only on the developer's machine ([ADR 0006](0006-run-locally.md)). For the final presentation, it must be available online, at a public HTTPS address, at no cost. Free plans are enough: the application is a demo with fake data, used by a few people at a time. The backend already has a Docker image and a CI pipeline ([ADR 0007](0007-run-with-docker-compose-and-check-with-github-actions.md)).

## Decision

Matriz is hosted on **Render**, with the database on **Neon**. Both are on free plans, in the Frankfurt (EU) region.

- **Frontend:** a Render Static Site, built from `frontend/` with `ng build`.
  - A rewrite rule forwards `/api/*` to the backend, so the browser sees one origin and the session cookie and CSRF work as they do locally ([ADR 0004](0004-use-email-password-with-server-side-sessions.md)). No CORS is needed.
  - Other paths load `index.html`, so Angular routes work after a reload.
- **Backend:** a Render Web Service, built from `backend/Dockerfile`, with its health check on `/api/actuator/health`.
- **Database:** a Neon PostgreSQL database. The connection uses SSL. Liquibase creates the schema and the demo data on the first start, as it does locally.
- **Configuration:**
  - The services are described in a Render Blueprint (`render.yaml`) in the repository.
  - The database URL, user and password are set only in the Render dashboard, never in the repository.
  - On Render, the session and CSRF cookies are `Secure`. Local runs keep them non-secure, because there is no HTTPS.
- **Deployment:** Render deploys `main` only after the GitHub Actions checks pass, so a commit that fails CI never reaches the live site.
- **Demo access:** the seeded demo accounts and their fake password (README) are used online too.

## Alternatives considered

- **Render PostgreSQL instead of Neon:** one dashboard for everything, but the free database expires after about 30 days.
- **Frontend as a Docker web service (nginx image):** reuses `frontend/Dockerfile`, but nginx would proxy to the backend's public URL, and free web services sleep when idle. A static site is free and doesn't sleep.
- **A private demo password for production:** keeps visitors out, but needs a second seed with a password kept outside the repository.
- **Deploy on every push, or by hand:** a failing commit could be deployed, or the site could fall behind `main`.

## Consequences

- Matriz has a public HTTPS address on a free `onrender.com` subdomain.
- The free backend sleeps after about 15 minutes without requests. The first request after that takes about a minute while it starts. Open the site a few minutes before a demo.
- Free plans have usage limits (Render hours, Neon storage and compute). They are enough for a demo.
- Anyone with the public demo password can sign in and change or delete the demo data. Only fake data is used. The database can be reset before the presentation, and Liquibase then loads the demo data again.
- Hosting, deployment and their accounts become something to maintain. The Render and Neon accounts belong to the developer and are set up in their dashboards.
- Local development and Docker Compose don't change.
