import { existsSync, readFileSync } from 'node:fs';
import { parseEnv } from 'node:util';

import { defineConfig, devices } from '@playwright/test';
import { defineBddConfig } from 'playwright-bdd';

/**
 * End-to-end tests (Practical Worksheet 04, Exercise 4), in their own environment: the
 * `db-e2e` database (docker-compose.yml, profile `e2e`, port 5433, recreated empty by
 * `npm run e2e`), and a backend (port 8081) and frontend (port 4201) started here. The
 * development database and servers (5432, 8080, 4200) aren't used.
 *
 * Scenarios are written in Gherkin (e2e/features) with step definitions in e2e/steps;
 * playwright-bdd turns them into Playwright tests in .features-gen (`bddgen`).
 */
const testDir = defineBddConfig({
  features: 'e2e/features/*.feature',
  steps: 'e2e/steps/*.ts',
});
const backendPort = 8081;
const frontendPort = 4201;
// The database name, as Docker Compose reads it from the repository's .env (see .env.example).
const envFile = '../.env';
const databaseName = (existsSync(envFile) && parseEnv(readFileSync(envFile, 'utf8'))['POSTGRES_DB']) || 'matriz';

export default defineConfig({
  testDir,
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: `http://localhost:${frontendPort}`,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      // The backend loads the schema and demo data into the empty E2E database (Liquibase).
      command: process.platform === 'win32' ? '.\\mvnw.cmd -q spring-boot:run' : './mvnw -q spring-boot:run',
      cwd: '../backend',
      env: {
        ...process.env,
        DB_URL: `jdbc:postgresql://localhost:5433/${databaseName}`,
        SERVER_PORT: String(backendPort),
      },
      url: `http://localhost:${backendPort}/api/actuator/health`,
      reuseExistingServer: false,
      timeout: 180_000,
      stdout: 'ignore',
      stderr: 'pipe',
    },
    {
      command: `npx ng serve --port ${frontendPort} --proxy-config proxy.e2e.json`,
      url: `http://localhost:${frontendPort}`,
      reuseExistingServer: false,
      timeout: 180_000,
    },
  ],
});
