import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { fieldMessages, toApiProblem } from '../../core/api-problem';
import { Member } from '../../core/projects/project';
import { Task } from '../../core/tasks/task';
import { TasksService } from '../../core/tasks/tasks.service';
import { initials } from '../../shared/initials';

/**
 * Assignees of a task, added and removed one at a time (US-10, EC-08, EC-09). The picker
 * only offers project members who aren't assigned yet (docs/ui/design-decisions.md).
 */
@Component({
  selector: 'app-assignees-panel',
  imports: [FormsModule],
  template: `
    <section class="card panel" aria-labelledby="assignees-title">
      <h2 id="assignees-title">Assignees</h2>
      @if (task().assignees.length) {
        <ul class="assignees">
          @for (user of task().assignees; track user.id) {
            <li>
              <span class="av" aria-hidden="true">{{ initials(user.name) }}</span>
              <span class="grow">{{ user.name }}</span>
              <button
                class="btn btn-ghost btn-sm"
                type="button"
                [attr.aria-label]="'Remove ' + user.name"
                [disabled]="pending()"
                (click)="unassign(user.id)"
              >✕</button>
            </li>
          }
        </ul>
      } @else {
        <p class="hint" role="status">No one is assigned yet.</p>
      }

      @if (eligible().length) {
        <form class="field" (ngSubmit)="assign()">
          <label class="label" for="assignee-picker">Assign a member</label>
          <div class="row">
            <select class="input grow" id="assignee-picker" name="userId" [(ngModel)]="selected">
              <option [ngValue]="null" disabled>Choose a member…</option>
              @for (member of eligible(); track member.userId) {
                <option [ngValue]="member.userId">{{ member.name }}{{ member.role === 'PROJECT_MANAGER' ? ' (PM)' : '' }}</option>
              }
            </select>
            <button class="btn btn-primary btn-sm" type="submit" [disabled]="pending() || selected === null">Assign</button>
          </div>
          <span class="hint">Only project members can be assigned.</span>
        </form>
      } @else {
        <p class="hint">All project members are assigned.</p>
      }

      @if (error(); as message) {
        <span class="err-msg" role="alert">{{ message }}</span>
      }
    </section>
  `,
  styles: `
    .row { display: flex; gap: 8px; }
    .grow { flex: 1; }
  `,
})
export class AssigneesPanel {
  private readonly tasks = inject(TasksService);

  readonly task = input.required<Task>();
  /** The project's members: the users who can be assigned. */
  readonly members = input.required<Member[]>();
  /** The task with its new assignees, as confirmed by the API. */
  readonly changed = output<Task>();

  protected readonly initials = initials;
  protected readonly pending = signal(false);
  protected readonly error = signal<string | null>(null);
  protected selected: number | null = null;

  protected readonly eligible = computed(() => {
    const assigned = new Set(this.task().assignees.map((user) => user.id));
    return this.members().filter((member) => !assigned.has(member.userId));
  });

  protected assign(): void {
    const userId = this.selected;
    if (userId === null || this.pending()) {
      return;
    }
    const task = this.task();
    this.start();
    this.tasks.assign(task.projectId, task.id, userId).subscribe({
      next: (updated) => {
        this.pending.set(false);
        this.selected = null;
        this.changed.emit(updated);
      },
      error: (error: unknown) => this.fail(error, 'Could not assign the member. Try again.'),
    });
  }

  protected unassign(userId: number): void {
    if (this.pending()) {
      return;
    }
    const task = this.task();
    this.start();
    this.tasks.unassign(task.projectId, task.id, userId).subscribe({
      next: () => {
        this.pending.set(false);
        this.changed.emit({ ...task, assignees: task.assignees.filter((user) => user.id !== userId) });
      },
      error: (error: unknown) => this.fail(error, 'Could not remove the assignment. Try again.'),
    });
  }

  private start(): void {
    this.pending.set(true);
    this.error.set(null);
  }

  private fail(error: unknown, fallback: string): void {
    this.pending.set(false);
    const problem = toApiProblem(error);
    this.error.set(fieldMessages(problem)['userId'] ?? problem.detail ?? fallback);
  }
}
