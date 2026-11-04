import { test as base } from 'playwright-bdd';

/**
 * Fixtures for the step definitions. `createdProjects` collects the projects a scenario
 * creates, and deletes them through the API at the end, even if the scenario failed.
 */
export const test = base.extend<{ createdProjects: string[] }>({
  createdProjects: async ({ page, context }, use) => {
    const ids: string[] = [];
    await use(ids);
    const xsrf = (await context.cookies()).find((cookie) => cookie.name === 'XSRF-TOKEN')?.value ?? '';
    for (const id of ids) {
      const response = await page.request.delete(`/api/projects/${id}`, { headers: { 'X-XSRF-TOKEN': xsrf } });
      if (response.status() !== 204) {
        throw new Error(`Could not delete project ${id} after the scenario: ${response.status()}`);
      }
    }
  },
});
