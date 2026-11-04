import { expect, type Page } from '@playwright/test';
import { createBdd } from 'playwright-bdd';

import { test } from './fixtures';

/**
 * Step definitions for e2e/features. They use the real labels, roles and accessible names
 * of the app (no test IDs), and web-first assertions instead of fixed waits.
 */
const { Given, When, Then } = createBdd(test);

Given('I am signed in as {string}', async ({ page }, email: string) => {
  // The demo password is only written in the README, so it comes from the environment.
  const password = process.env['E2E_PASSWORD'];
  if (!password) {
    throw new Error('Set E2E_PASSWORD to the demo account password (see README, Demo accounts).');
  }
  await page.goto('/sign-in');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText(/^Hello, .+/);
});

Given('I have created the project {string}', async ({ page, createdProjects }, name: string) => {
  await page.getByRole('navigation', { name: 'Main navigation' }).getByRole('link', { name: 'Projects', exact: true }).click();
  await page.getByRole('button', { name: '+ New Project' }).first().click();
  const dialog = page.getByRole('dialog', { name: 'New Project' });
  await dialog.getByLabel('Name').fill(name);
  await dialog.getByRole('button', { name: 'Create Project' }).click();
  await expect(dialog).toBeHidden();

  await page.getByRole('link', { name: new RegExp(`^${escapeRegExp(name)}, you are project manager`) }).click();
  await expect(page.getByRole('heading', { level: 1, name })).toBeVisible();
  createdProjects.push(new URL(page.url()).pathname.split('/').pop()!);
});

When('I create the task {string}', async ({ page }, title: string) => {
  await page.getByRole('button', { name: '+ New Task' }).first().click();
  const dialog = page.getByRole('dialog', { name: 'New Task' });
  await dialog.getByLabel('Title').fill(title);
  await dialog.getByRole('button', { name: 'Create Task' }).click();
  await expect(dialog).toBeHidden();
});

Then('I see {string} in the {string} column', async ({ page }, title: string, column: string) => {
  await expect(boardColumn(page, column).getByRole('link', { name: title, exact: true })).toBeVisible();
});

Then('the {string} and {string} columns are empty', async ({ page }, first: string, second: string) => {
  for (const column of [first, second]) {
    await expect(page.getByRole('listitem', { name: `${column}, 0 tasks`, exact: true })).toBeVisible();
  }
});

/** A board column; its accessible name is "<status>, <n> task(s)". */
function boardColumn(page: Page, column: string) {
  return page.getByRole('listitem', { name: new RegExp(`^${escapeRegExp(column)}, \\d+ tasks?$`) });
}

function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
