# Matriz frontend image (ADR 0007): the Angular app served by nginx, which also forwards
# /api to the backend so the browser sees one origin (session cookie, ADR 0004).

# Build: install the locked dependencies and build the production bundle.
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
# npm as declared in package.json ("packageManager"): newer npm rejects the lock file.
RUN npm install -g npm@11.6.2 && npm ci
COPY . .
RUN npx ng build

# Run: nginx as a non-root user (the unprivileged image listens on 8080).
FROM nginxinc/nginx-unprivileged:alpine
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/matriz-frontend/browser /usr/share/nginx/html
EXPOSE 8080
