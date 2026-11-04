import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end tests (Practical Worksheet 04, Exercise 4), in their own environment: the
 * `db-e2e` database (docker-compose.yml, profile `e2e`, port 5433, recreated empty by
 * `npm run e2e`), and a backend (port 8081) and frontend (port 4201) started here. The
 * development database and servers (5432, 8080, 4200) aren't used.
 */
const backendPort = 8081;
const frontendPort = 4201;

export default defineConfig({
  testDir: './e2e',
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
        DB_URL: 'jdbc:postgresql://localhost:5433/matriz',
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
