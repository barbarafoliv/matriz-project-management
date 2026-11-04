import { Component, ElementRef, Injector, OnInit, afterNextRender, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PRIORITIES, Priority, STATUSES, Task, TaskStatus, formatDate } from '../../core/tasks/task';
import { TasksService } from '../../core/tasks/tasks.service';
import { PriorityBadge } from '../../shared/badges';
import { initials } from '../../shared/initials';
import { NewTaskDialog } from './new-task-dialog';

type BoardState = 'loading' | 'ready' | 'error';

/**
 * The project's board: one column per status, filters by status and priority, and New Task
 * (US-07, US-08, US-13, EC-11).
 */
@Component({
  selector: 'app-task-board',
  imports: [RouterLink, NewTaskDialog, PriorityBadge],
  templateUrl: './task-board.html',
})
export class TaskBoard implements OnInit {
  private readonly tasksService = inject(TasksService);

  readonly projectId = input.required<number>();
  /** A task to highlight and scroll to, when coming from "My tasks" (docs/ui/design-decisions.md, Q1). */
  readonly highlight = input<number | null>(null);

  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);

  protected readonly statuses = STATUSES;
  protected readonly priorities = PRIORITIES;
  protected readonly formatDate = formatDate;
  protected readonly initials = initials;

  protected readonly state = signal<BoardState>('loading');
  protected readonly tasks = signal<Task[]>([]);
  protected readonly status = signal<TaskStatus | ''>('');
  protected readonly priority = signal<Priority | ''>('');
  protected readonly creating = signal(false);

  /** With a status filter, only that status's column is shown. */
  protected readonly columns = computed(() =>
    this.statuses
      .filter((column) => !this.status() || column.value === this.status())
      .map((column) => ({ ...column, tasks: this.tasks().filter((task) => task.status === column.value) })),
  );
  protected readonly filtered = computed(() => !!this.status() || !!this.priority());

  ngOnInit(): void {
    this.load();
  }

  /** Asks the API for the tasks that match the filters. */
  protected load(): void {
    this.state.set('loading');
    this.tasksService
      .list(this.projectId(), { status: this.status() || null, priority: this.priority() || null })
      .subscribe({
        next: (tasks) => {
          this.tasks.set(tasks);
          this.state.set('ready');
          this.scrollToHighlight();
        },
        error: () => this.state.set('error'),
      });
  }

  protected setStatus(value: string): void {
    this.status.set(value as TaskStatus | '');
    this.load();
  }

  protected setPriority(value: string): void {
    this.priority.set(value as Priority | '');
    this.load();
  }

  protected clearFilters(): void {
    this.status.set('');
    this.priority.set('');
    this.load();
  }

  protected onCreated(): void {
    this.creating.set(false);
    this.load();
  }

  private scrollToHighlight(): void {
    if (this.highlight() == null) {
      return;
    }
    afterNextRender(
      () => this.element.nativeElement.querySelector<HTMLElement>('.tc.highlight')?.scrollIntoView?.({ block: 'center' }),
      { injector: this.injector },
    );
  }

  /** Up to 3 initials on a card, then "+N" (docs/ui/design-decisions.md). */
  protected shownAssignees(task: Task) {
    return task.assignees.slice(0, 3);
  }

  protected assigneesLabel(task: Task): string {
    return task.assignees.length ? 'Assigned to ' + task.assignees.map((user) => user.name).join(', ') : 'Unassigned';
  }
}
