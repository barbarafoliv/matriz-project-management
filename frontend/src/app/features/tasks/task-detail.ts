import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { fieldMessages, toApiProblem } from '../../core/api-problem';
import { Project } from '../../core/projects/project';
import { ProjectsService } from '../../core/projects/projects.service';
import { PRIORITIES, Priority, STATUSES, Task, TaskStatus, formatDate, priorityLabel, statusLabel } from '../../core/tasks/task';
import { TasksService } from '../../core/tasks/tasks.service';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { initials } from '../../shared/initials';
import { trimmedMinLength } from '../../shared/validators';

type PageState = 'loading' | 'ready' | 'not-found' | 'error';

/**
 * Task detail page: details, status, edit and delete (US-04, US-05, US-08). Assignees are
 * shown here; assigning them comes with US-10.
 */
@Component({
  selector: 'app-task-detail',
  imports: [RouterLink, ReactiveFormsModule, ConfirmDialog],
  templateUrl: './task-detail.html',
})
export class TaskDetail implements OnInit {
  private readonly projects = inject(ProjectsService);
  private readonly tasks = inject(TasksService);
  private readonly router = inject(Router);

  /** From the route, `/projects/:projectId/tasks/:taskId`. */
  readonly projectId = input.required<string>();
  readonly taskId = input.required<string>();

  protected readonly statuses = STATUSES;
  protected readonly priorities = PRIORITIES;
  protected readonly formatDate = formatDate;
  protected readonly statusLabel = statusLabel;
  protected readonly priorityLabel = priorityLabel;
  protected readonly initials = initials;

  protected readonly state = signal<PageState>('loading');
  protected readonly project = signal<Project | null>(null);
  protected readonly task = signal<Task | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    title: ['', [trimmedMinLength(3), Validators.maxLength(150)]],
    description: ['', [Validators.maxLength(2000)]],
    priority: ['MEDIUM' as Priority],
    dueDate: [''],
  });
  protected readonly saving = signal(false);
  protected readonly saved = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly serverErrors = signal<Record<string, string>>({});
  protected readonly statusPending = signal(false);
  protected readonly statusError = signal<string | null>(null);
  protected readonly confirmingDelete = signal(false);
  protected readonly deletePending = signal(false);
  protected readonly deleteError = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    const projectId = Number(this.projectId());
    const taskId = Number(this.taskId());
    if (!(projectId >= 1 && taskId >= 1)) {
      this.state.set('not-found');
      return;
    }
    this.state.set('loading');
    forkJoin({ project: this.projects.get(projectId), task: this.tasks.get(projectId, taskId) }).subscribe({
      next: ({ project, task }) => {
        this.project.set(project);
        this.showTask(task);
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

  protected titleError(): string | null {
    const control = this.form.controls.title;
    if (control.invalid) {
      return control.hasError('required') ? 'Title must not be empty.' : 'Title must be between 3 and 150 characters.';
    }
    return this.serverErrors()['title'] ?? null;
  }

  /** A status button saves straight away (docs/ui/design-decisions.md). */
  protected changeStatus(status: TaskStatus): void {
    const task = this.task();
    if (!task || task.status === status || this.statusPending()) {
      return;
    }
    this.statusPending.set(true);
    this.statusError.set(null);
    this.tasks.changeStatus(task.projectId, task.id, status).subscribe({
      next: (updated) => {
        this.statusPending.set(false);
        // Only the status and late flag change; unsaved edits in the form are kept.
        this.task.set(updated);
      },
      error: (error: unknown) => {
        this.statusPending.set(false);
        this.statusError.set(toApiProblem(error).detail ?? 'Could not change the status. Try again.');
      },
    });
  }

  protected save(): void {
    const task = this.task();
    if (!task || this.saving()) {
      return;
    }
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }
    this.saving.set(true);
    this.saved.set(false);
    this.saveError.set(null);
    this.serverErrors.set({});
    const { title, description, priority, dueDate } = this.form.getRawValue();
    this.tasks
      .update(task.projectId, task.id, {
        title: title.trim(),
        description: description.trim() || null,
        priority,
        dueDate: dueDate || null,
      })
      .subscribe({
        next: (updated) => {
          this.saving.set(false);
          this.saved.set(true);
          this.showTask(updated);
        },
        error: (error: unknown) => {
          // Keep what was typed, so it can be corrected.
          this.saving.set(false);
          const problem = toApiProblem(error);
          if (problem.code === 'VALIDATION_FAILED') {
            this.serverErrors.set(fieldMessages(problem));
          } else {
            this.saveError.set(problem.detail ?? 'Could not save the task. Try again.');
          }
        },
      });
  }

  protected cancelEdits(): void {
    const task = this.task();
    if (task) {
      this.showTask(task);
    }
  }

  protected deleteTask(): void {
    const task = this.task();
    if (!task) {
      return;
    }
    this.deletePending.set(true);
    this.deleteError.set(null);
    this.tasks.delete(task.projectId, task.id).subscribe({
      next: () => this.router.navigate(['/projects', task.projectId]),
      error: (error: unknown) => {
        this.deletePending.set(false);
        this.deleteError.set(toApiProblem(error).detail ?? 'Could not delete the task. Try again.');
      },
    });
  }

  protected closeDelete(): void {
    this.confirmingDelete.set(false);
    this.deletePending.set(false);
    this.deleteError.set(null);
  }

  private showTask(task: Task): void {
    this.task.set(task);
    this.form.reset({
      title: task.title,
      description: task.description ?? '',
      priority: task.priority,
      dueDate: task.dueDate ?? '',
    });
  }
}
