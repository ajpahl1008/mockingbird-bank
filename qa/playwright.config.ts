import { defineConfig } from '@playwright/test';

// Interactive QA against a real, already-running Mockingbird Bank instance -
// see ../scripts/run-interactive-qa.sh, which starts the app and Postgres
// before invoking `npx playwright test` here.
export default defineConfig({
  testDir: './tests',
  timeout: 30_000,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: process.env.QA_BASE_URL ?? 'http://localhost:8080',
    // Vaadin's dev-mode server can take a while to JIT-compile the first
    // request against a view it hasn't served yet.
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { browserName: 'chromium' },
    },
  ],
});
