# 0006 - Run Locally

## Status

Accepted

## Context

Matriz is an academic project developed and used by one person. There is no requirement to make the application available over the internet or to other users.

## Decision

Matriz will run locally on the developer's machine. There is no external hosting.

- The Spring Boot backend, Angular frontend and PostgreSQL database all run on the developer's machine.
- No deployment pipeline, cloud account or server configuration is needed.

## Alternatives considered

- **A PaaS (e.g. Render, Fly.io):** would make the application reachable from anywhere, but adds unnecessary complexity for an academic project.
- **A VPS:** more control, but requires server setup and ongoing maintenance.

## Consequences

- Setup is simple: install JDK, Node.js and PostgreSQL, then run the application.
- The application is only available when the developer's machine is running.
- No security hardening for public exposure (HTTPS, firewall rules) is needed for this scope.
