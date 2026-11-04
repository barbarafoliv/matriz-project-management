import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api.config';
import { MyTask, Task, TaskFilters, TaskRequest, TaskStatus } from './task';

/** Tasks API (US-04, US-05, US-07, US-13). */
@Injectable({ providedIn: 'root' })
export class TasksService {
  private readonly http = inject(HttpClient);

  private url(projectId: number): string {
    return `${API_BASE_URL}/projects/${projectId}/tasks`;
  }

  /** The project's tasks, filtered by the API (docs/ui/design-decisions.md, Filters). */
  list(projectId: number, filters: TaskFilters = {}): Observable<Task[]> {
    let params = new HttpParams();
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.priority) {
      params = params.set('priority', filters.priority);
    }
    return this.http.get<Task[]>(this.url(projectId), { params });
  }

  get(projectId: number, taskId: number): Observable<Task> {
    return this.http.get<Task>(`${this.url(projectId)}/${taskId}`);
  }

  create(projectId: number, task: TaskRequest): Observable<Task> {
    return this.http.post<Task>(this.url(projectId), task);
  }

  update(projectId: number, taskId: number, task: TaskRequest): Observable<Task> {
    return this.http.put<Task>(`${this.url(projectId)}/${taskId}`, task);
  }

  changeStatus(projectId: number, taskId: number, status: TaskStatus): Observable<Task> {
    return this.http.put<Task>(`${this.url(projectId)}/${taskId}/status`, { status });
  }

  delete(projectId: number, taskId: number): Observable<void> {
    return this.http.delete<void>(`${this.url(projectId)}/${taskId}`);
  }

  /** Assigns a project member; the API returns the task with its assignees (US-10). */
  assign(projectId: number, taskId: number, userId: number): Observable<Task> {
    return this.http.post<Task>(`${this.url(projectId)}/${taskId}/assignees`, { userId });
  }

  unassign(projectId: number, taskId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.url(projectId)}/${taskId}/assignees/${userId}`);
  }

  /** Tasks assigned to the signed-in user in all their projects (US-06). */
  myTasks(): Observable<MyTask[]> {
    return this.http.get<MyTask[]>(`${API_BASE_URL}/users/me/tasks`);
  }
}
