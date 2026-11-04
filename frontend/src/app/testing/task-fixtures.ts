import { Task } from '../core/tasks/task';

/** A task for tests, with sensible defaults. Kept out of spec files so importing it doesn't run other tests. */
export const aTask = (overrides: Partial<Task>): Task => ({
  id: 1,
  projectId: 7,
  title: 'A task',
  description: null,
  assignees: [],
  dueDate: null,
  priority: 'MEDIUM',
  status: 'TODO',
  late: false,
  createdAt: '',
  updatedAt: '',
  ...overrides,
});
