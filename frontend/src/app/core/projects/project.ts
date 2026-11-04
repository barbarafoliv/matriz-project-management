/** Types from docs/openapi.yaml (Projects). */

export type Role = 'PROJECT_MANAGER' | 'MEMBER';

export interface TaskCounts {
  todo: number;
  inProgress: number;
  done: number;
}

export interface Project {
  id: number;
  name: string;
  description: string | null;
  myRole: Role;
  taskCounts: TaskCounts;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectRequest {
  name: string;
  description: string | null;
}

export interface Member {
  userId: number;
  name: string;
  email: string;
  role: Role;
}
