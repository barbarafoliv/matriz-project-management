import { Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { fieldMessages, toApiProblem } from '../../core/api-problem';
import { PRIORITIES, Priority, Task } from '../../core/tasks/task';
import { TasksService } from '../../core/tasks/tasks.service';
import { Modal } from '../../shared/modal';
import { trimmedMinLength } from '../../shared/validators';

/** New Task (US-04, EC-03). Assignees are added on the task page (docs/ui/design-decisions.md). */
@Component({
  selector: 'app-new-task-dialog',
  imports: [Modal, ReactiveFormsModule],
  template: `
    <app-modal title="New Task" (closed)="closed.emit()">
      <form id="task-form" class="form" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        @if (error(); as message) {
          <div class="alert alert-error" role="alert">{{ message }}</div>
        }
        <div class="field">
          <label class="label" for="task-title">Title <span aria-hidden="true">*</span></label>
          <input
            class="input"
            id="task-title"
            type="text"
            formControlName="title"
            maxlength="150"
            aria-required="true"
            aria-describedby="task-title-hint"
            [attr.aria-invalid]="!!titleError()"
          />
          @if (titleError(); as message) {
            <span class="err-msg" role="alert">{{ message }}</span>
          } @else {
            <span class="hint" id="task-title-hint">3 to 150 characters.</span>
          }
        </div>
        <div class="field">
          <label class="label" for="task-description">Description</label>
          <textarea class="input" id="task-description" rows="2" maxlength="2000" formControlName="description" placeholder="Optional…"></textarea>
          @if (serverErrors()['description']; as message) {
            <span class="err-msg" role="alert">{{ message }}</span>
          }
        </div>
        <div class="grid2">
          <div class="field">
            <label class="label" for="task-priority">Priority</label>
            <select class="input" id="task-priority" formControlName="priority">
              @for (priority of priorities; track priority.value) {
                <option [value]="priority.value">{{ priority.label }}{{ priority.value === 'MEDIUM' ? ' (default)' : '' }}</option>
              }
            </select>
          </div>
          <div class="field">
            <label class="label" for="task-due">Due date</label>
            <input class="input" id="task-due" type="date" formControlName="dueDate" />
          </div>
        </div>
        <span class="hint">New tasks start as To Do. Assignees are added on the task page.</span>
      </form>
      <div modal-footer>
        <button class="btn" type="button" (click)="closed.emit()">Cancel</button>
        <button class="btn btn-primary" type="submit" form="task-form" [disabled]="pending()">Create Task</button>
      </div>
    </app-modal>
  `,
})
export class NewTaskDialog {
  private readonly tasks = inject(TasksService);

  readonly projectId = input.required<number>();
  readonly created = output<Task>();
  readonly closed = output<void>();

  protected readonly priorities = PRIORITIES;
  protected readonly form = inject(FormBuilder).nonNullable.group({
    title: ['', [trimmedMinLength(3), Validators.maxLength(150)]],
    description: ['', [Validators.maxLength(2000)]],
    priority: ['MEDIUM' as Priority],
    dueDate: [''],
  });
  protected readonly pending = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverErrors = signal<Record<string, string>>({});
  private readonly submitted = signal(false);

  protected titleError(): string | null {
    const control = this.form.controls.title;
    if (control.invalid && (control.touched || this.submitted())) {
      return control.hasError('required') ? 'Title must not be empty.' : 'Title must be between 3 and 150 characters.';
    }
    return this.serverErrors()['title'] ?? null;
  }

  protected submit(): void {
    if (this.pending()) {
      return;
    }
    this.submitted.set(true);
    if (this.form.invalid) {
      return;
    }
    this.pending.set(true);
    this.error.set(null);
    this.serverErrors.set({});
    const { title, description, priority, dueDate } = this.form.getRawValue();
    this.tasks
      .create(this.projectId(), {
        title: title.trim(),
        description: description.trim() || null,
        priority,
        dueDate: dueDate || null,
      })
      .subscribe({
        next: (task) => this.created.emit(task),
        error: (error: unknown) => {
          // Keep what was typed, so it can be corrected.
          this.pending.set(false);
          const problem = toApiProblem(error);
          if (problem.code === 'VALIDATION_FAILED') {
            this.serverErrors.set(fieldMessages(problem));
          } else {
            this.error.set(problem.detail ?? 'Could not create the task. Try again.');
          }
        },
      });
  }
}
