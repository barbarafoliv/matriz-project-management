import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api.config';
import { Member, Project, ProjectRequest } from './project';

/** Projects and members API (US-02, US-03, US-12). */
@Injectable({ providedIn: 'root' })
export class ProjectsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/projects`;

  /** The caller's projects, optionally only those whose name contains `search`. */
  list(search = ''): Observable<Project[]> {
    const text = search.trim();
    const params = text ? new HttpParams().set('q', text) : undefined;
    return this.http.get<Project[]>(this.url, { params });
  }

  get(projectId: number): Observable<Project> {
    return this.http.get<Project>(`${this.url}/${projectId}`);
  }

  create(project: ProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.url, project);
  }

  update(projectId: number, project: ProjectRequest): Observable<Project> {
    return this.http.put<Project>(`${this.url}/${projectId}`, project);
  }

  delete(projectId: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${projectId}`);
  }

  members(projectId: number): Observable<Member[]> {
    return this.http.get<Member[]>(`${this.url}/${projectId}/members`);
  }

  addMember(projectId: number, email: string): Observable<Member> {
    return this.http.post<Member>(`${this.url}/${projectId}/members`, { email });
  }

  removeMember(projectId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${projectId}/members/${userId}`);
  }
}
