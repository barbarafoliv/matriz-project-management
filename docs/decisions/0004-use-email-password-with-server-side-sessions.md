# 0004 - Use Email and Password with Server-Side Sessions

## Status

Accepted

## Context

Users sign in and out with an email and password (FR-01, US-01). In the MVP, accounts are demo accounts created by a seed; public sign-up is an optional extension (FR-18). Only signed-in users can use the application. Passwords must be stored securely, repeated failed sign-in attempts must be limited, and inactive sessions must expire (NFR-02). When an account is deleted, the user must be signed out (US-09). Only the name and email of each user are collected (constraints §3).

## Decision

Users sign in with their email and password. After signing in, the application keeps a session on the server, identified by a secure cookie sent with each request.

- Passwords are stored only as secure hashes.
- Sessions expire after a period of inactivity, and are ended on sign-out and on account deletion.
- Repeated failed sign-in attempts are limited.

Exact limits, such as the inactivity timeout and the number of failed attempts, are set in the technical design (NFR-02).

## Alternatives considered

- **Tokens (JWT):** nothing stored on the server, but expiring inactive sessions and signing users out immediately are harder.
- **External identity provider (e.g. Keycloak, Auth0):** sign-in security handled by another system, but it adds a system to run or pay for, and stores personal data with a third party.

## Consequences

- Inactive sessions can be expired, and users signed out immediately, by ending their session on the server.
- No personal data is shared with a third party.
- Sessions and failed sign-in attempts must be stored by the application. Where they are stored will be decided with the application technology.
- The cookie must be protected against misuse, for example by sending it only over HTTPS and not exposing it to scripts.

## Notes

- Sessions are stored in PostgreSQL with Spring Session JDBC, and failed sign-in attempts in an application table ([ADR 0005](0005-use-spring-boot-and-angular.md), [database.md](../database.md)).
- The application runs locally without HTTPS ([ADR 0006](0006-run-locally.md)), so the cookie is `HttpOnly` and `SameSite=Lax`, but not `Secure`.
- Updated after the MVP scope moved sign-up to an optional extension. The decision is unchanged.
