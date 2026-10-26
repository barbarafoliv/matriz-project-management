# 0003 - Use REST Between Clients and API

## Status

Accepted

## Context

Matriz clients need to communicate with the application's API to manage accounts, projects and tasks. The operations are mostly simple create, read, update and delete actions on these resources (FR-01 to FR-08, FR-14). Only signed-in users can use the API, and they only see their own projects (NFR-02). Real-time chat and email notifications are out of the initial scope.

## Decision

Clients will communicate with the API using REST over HTTPS, with JSON as the data format.

- Resources such as projects, members and tasks are exposed as URLs, and standard HTTP methods are used to act on them (GET, POST, PUT/PATCH, DELETE).
- The API uses standard HTTP status codes to report results and errors.

## Alternatives considered

- **GraphQL:** flexible queries, but adds complexity that the MVP's simple operations don't need.
- **gRPC:** efficient, but not well suited to browser clients and harder to inspect and debug.

## Consequences

- REST is simple, widely known and well supported by frameworks, tools and browsers.
- The API is easy to test and document.
- Some screens may need several requests, or dedicated endpoints, to load all their data.
- Real-time updates, if needed later, will require a separate mechanism.
