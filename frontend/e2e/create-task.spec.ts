import { expect, test } from '@playwright/test';

/**
 * Sign in → open a project → create a task → see it on the board (US-01, US-02, US-04,
 * US-07; docs/test-plan.md T12). The test creates its own project and deletes it at the
 * end, so the demo data is left as it was and the test can be run again.
 */

const email = 'alice@example.com';
const password = process.env['E2E_PASSWORD'];

let createdProjectId: string | null = null;

test.beforeAll(() => {
  if (!password) {
    throw new Error('Set E2E_PASSWORD to the demo account password (see README, Demo accounts).');
  }
});

test.afterEach(async ({ page, context }) => {
  if (!createdProjectId) {
    return;
  }
  // Clean up through the API, even if the test failed half-way.
  const xsrf = (await context.cookies()).find((cookie) => cookie.name === 'XSRF-TOKEN')?.value ?? '';
  const response = await page.request.delete(`/api/projects/${createdProjectId}`, {
    headers: { 'X-XSRF-TOKEN': xsrf },
  });
  expect(response.status(), 'project clean-up').toBe(204);
  createdProjectId = null;
});

test('a member creates a task and sees it in the To Do column', async ({ page }) => {
  const suffix = Date.now().toString(36);
  const projectName = `E2E project ${suffix}`;
  const taskTitle = `E2E task ${suffix}`;

  // Sign in.
  await page.goto('/sign-in');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(password!);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Hello, Alice Example');

  // Create a project and open it.
  await page.getByRole('navigation', { name: 'Main navigation' }).getByRole('link', { name: 'Projects', exact: true }).click();
  await page.getByRole('button', { name: '+ New Project' }).first().click();
  const projectDialog = page.getByRole('dialog', { name: 'New Project' });
  await projectDialog.getByLabel('Name').fill(projectName);
  await projectDialog.getByRole('button', { name: 'Create Project' }).click();
  await expect(projectDialog).toBeHidden();

  await page.getByRole('link', { name: new RegExp(`^${projectName}, you are project manager`) }).click();
  await expect(page.getByRole('heading', { level: 1, name: projectName })).toBeVisible();
  createdProjectId = new URL(page.url()).pathname.split('/').pop()!;

  // Create a task.
  await page.getByRole('button', { name: '+ New Task' }).first().click();
  const taskDialog = page.getByRole('dialog', { name: 'New Task' });
  await taskDialog.getByLabel('Title').fill(taskTitle);
  await taskDialog.getByRole('button', { name: 'Create Task' }).click();
  await expect(taskDialog).toBeHidden();

  // The task is on the board, in To Do, without reloading the page.
  const toDo = page.getByRole('listitem', { name: /^To Do, 1 task$/ });
  await expect(toDo.getByRole('link', { name: taskTitle })).toBeVisible();
  await expect(page.getByRole('listitem', { name: /^In Progress, 0 tasks$/ })).toBeVisible();
  await expect(page.getByRole('listitem', { name: /^Done, 0 tasks$/ })).toBeVisible();
});
