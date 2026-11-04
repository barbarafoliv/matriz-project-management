/** Types from docs/openapi.yaml (Tasks). */

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH';

export const STATUSES: { value: TaskStatus; label: string }[] = [
  { value: 'TODO', label: 'To Do' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'DONE', label: 'Done' },
];

export const PRIORITIES: { value: Priority; label: string }[] = [
  { value: 'LOW', label: 'Low' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HIGH', label: 'High' },
];

export interface UserSummary {
  id: number;
  name: string;
}

export interface Task {
  id: number;
  projectId: number;
  title: string;
  description: string | null;
  assignees: UserSummary[];
  dueDate: string | null;
  priority: Priority;
  status: TaskStatus;
  late: boolean;
  createdAt: string;
  updatedAt: string;
}

/** A task in "My tasks", with its project (docs/openapi.yaml, MyTask). */
export interface MyTask extends Task {
  project: { id: number; name: string };
}

/** A comment (docs/openapi.yaml, Comment). `author` is null if their account was deleted. */
export interface Comment {
  id: number;
  taskId: number;
  content: string;
  author: UserSummary | null;
  createdAt: string;
}

export interface TaskRequest {
  title: string;
  description: string | null;
  dueDate: string | null;
  priority: Priority;
}

export interface TaskFilters {
  status?: TaskStatus | null;
  priority?: Priority | null;
}

export function statusLabel(status: TaskStatus): string {
  return STATUSES.find((item) => item.value === status)!.label;
}

export function priorityLabel(priority: Priority): string {
  return PRIORITIES.find((item) => item.value === priority)!.label;
}

/** Shows a date and time from the API (ISO, UTC) in local time, as 15/11/2026 14:05. */
export function formatDateTime(value: string): string {
  const date = new Date(value);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/** Shows an ISO date (2026-11-15) as 15/11/2026, without time zone conversions. */
export function formatDate(date: string | null): string {
  return date ? date.split('-').reverse().join('/') : '—';
}
