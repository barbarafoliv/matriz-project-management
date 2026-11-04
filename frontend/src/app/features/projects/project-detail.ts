import { Component, OnInit, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { toApiProblem } from '../../core/api-problem';
import { Member, Project } from '../../core/projects/project';
import { ProjectsService } from '../../core/projects/projects.service';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { initials } from '../../shared/initials';
import { MembersDialog } from './members-dialog';
import { ProjectFormDialog } from './project-form-dialog';
import { TaskBoard } from '../tasks/task-board';

type PageState = 'loading' | 'ready' | 'not-found' | 'error';

/** Project detail: data, members, edit and delete, and the task board (US-02, US-03, US-07, EC-04). */
@Component({
  selector: 'app-project-detail',
  imports: [RouterLink, ConfirmDialog, MembersDialog, ProjectFormDialog, TaskBoard],
  templateUrl: './project-detail.html',
})
export class ProjectDetail implements OnInit {
  private readonly projects = inject(ProjectsService);
  private readonly router = inject(Router);

  /** From the route, `/projects/:projectId`. */
  readonly projectId = input.required<string>();

  protected readonly initials = initials;
  protected readonly state = signal<PageState>('loading');
  protected readonly project = signal<Project | null>(null);
  protected readonly members = signal<Member[]>([]);
  protected readonly dialog = signal<'edit' | 'delete' | 'members' | null>(null);
  protected readonly deletePending = signal(false);
  protected readonly deleteError = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    const id = Number(this.projectId());
    if (!Number.isInteger(id) || id < 1) {
      this.state.set('not-found');
      return;
    }
    this.state.set('loading');
    forkJoin({ project: this.projects.get(id), members: this.projects.members(id) }).subscribe({
      next: ({ project, members }) => {
        this.project.set(project);
        this.members.set(members);
        this.state.set('ready');
      },
      error: (error: unknown) => {
        const status = toApiProblem(error).status;
        this.state.set(status === 404 || status === 400 ? 'not-found' : 'error');
      },
    });
  }

  protected isManager(): boolean {
    return this.project()?.myRole === 'PROJECT_MANAGER';
  }

  protected onSaved(project: Project): void {
    this.project.set(project);
    this.dialog.set(null);
  }

  protected reloadMembers(): void {
    const project = this.project();
    if (project) {
      this.projects.members(project.id).subscribe({ next: (members) => this.members.set(members) });
    }
  }

  protected deleteProject(): void {
    const project = this.project();
    if (!project) {
      return;
    }
    this.deletePending.set(true);
    this.deleteError.set(null);
    this.projects.delete(project.id).subscribe({
      next: () => this.router.navigate(['/projects']),
      error: (error: unknown) => {
        this.deletePending.set(false);
        this.deleteError.set(toApiProblem(error).detail ?? 'Could not delete the project. Try again.');
      },
    });
  }

  protected closeDialog(): void {
    this.dialog.set(null);
    this.deletePending.set(false);
    this.deleteError.set(null);
  }
}
