import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { Project } from '../../core/projects/project';
import { ProjectsService } from '../../core/projects/projects.service';
import { ProjectCard } from '../projects/project-card';

type PageState = 'loading' | 'ready' | 'error';

/**
 * Dashboard, the home screen after sign-in: the signed-in user, their projects and each
 * project's task counts per status, with a link to each project (FR-17, US-14). The counts
 * come from the API and cover all of a project's tasks; they are loaded again each time the
 * dashboard opens, so they follow any change.
 */
@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, ProjectCard],
  template: `
    <div class="ph">
      <div>
        <h1 class="page-title">Hello, {{ auth.currentUser()?.name }}</h1>
        <p class="muted">Your projects and their tasks per status.</p>
      </div>
      <a class="btn" routerLink="/my-tasks">My Tasks</a>
    </div>

    @switch (state()) {
      @case ('loading') {
        <div class="proj-grid" aria-busy="true" aria-label="Loading your projects">
          @for (i of [1, 2, 3]; track i) {
            <div class="skel" style="height: 120px"></div>
          }
        </div>
      }
      @case ('error') {
        <div class="alert alert-error" role="alert">
          Could not load your projects. <button class="btn-link" type="button" (click)="load()">Try again</button>
        </div>
      }
      @case ('ready') {
        @if (projects().length === 0) {
          <div class="empty" role="status">
            <h2>No projects yet</h2>
            <p>Create your first project to start organising your team's work.</p>
            <a class="btn btn-primary" routerLink="/projects">Go to Projects</a>
          </div>
        } @else {
          <div class="proj-grid">
            @for (project of projects(); track project.id) {
              <a app-project-card class="proj-card" [routerLink]="['/projects', project.id]" [project]="project"></a>
            }
          </div>
        }
      }
    }
  `,
})
export class Dashboard implements OnInit {
  private readonly projectsService = inject(ProjectsService);
  protected readonly auth = inject(AuthService);

  protected readonly state = signal<PageState>('loading');
  protected readonly projects = signal<Project[]>([]);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.projectsService.list().subscribe({
      next: (projects) => {
        this.projects.set(projects);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }
}
