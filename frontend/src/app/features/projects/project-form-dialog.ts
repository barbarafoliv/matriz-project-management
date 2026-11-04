import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { fieldMessages, toApiProblem } from '../../core/api-problem';
import { Project } from '../../core/projects/project';
import { ProjectsService } from '../../core/projects/projects.service';
import { Modal } from '../../shared/modal';

/** Rejects text made only of spaces, or shorter than `min` once trimmed (EC-03). */
function trimmedMinLength(min: number) {
  return (control: { value: string }) => {
    const length = (control.value ?? '').trim().length;
    if (length === 0) {
      return { required: true };
    }
    return length < min ? { minlength: true } : null;
  };
}

/** New Project and Edit Project (US-02, EC-03). */
@Component({
  selector: 'app-project-form-dialog',
  imports: [Modal, ReactiveFormsModule],
  template: `
    <app-modal [title]="project() ? 'Edit Project' : 'New Project'" (closed)="closed.emit()">
      <form id="project-form" class="form" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        @if (error(); as message) {
          <div class="alert alert-error" role="alert">{{ message }}</div>
        }
        <div class="field">
          <label class="label" for="project-name">Name <span aria-hidden="true">*</span></label>
          <input
            class="input"
            id="project-name"
            type="text"
            formControlName="name"
            maxlength="100"
            aria-required="true"
            aria-describedby="project-name-hint"
            [attr.aria-invalid]="!!nameError()"
          />
          @if (nameError(); as message) {
            <span class="err-msg" role="alert">{{ message }}</span>
          } @else {
            <span class="hint" id="project-name-hint">3 to 100 characters.</span>
          }
        </div>
        <div class="field">
          <label class="label" for="project-description">Description</label>
          <textarea
            class="input"
            id="project-description"
            rows="3"
            formControlName="description"
            maxlength="1000"
            placeholder="Optional…"
          ></textarea>
          @if (serverErrors()['description']; as message) {
            <span class="err-msg" role="alert">{{ message }}</span>
          }
        </div>
        @if (!project()) {
          <div class="alert alert-info" role="note">You will become the <strong>project manager</strong> of this project.</div>
        }
      </form>
      <div modal-footer>
        <button class="btn" type="button" (click)="closed.emit()">Cancel</button>
        <button class="btn btn-primary" type="submit" form="project-form" [disabled]="pending()">
          {{ project() ? 'Save' : 'Create Project' }}
        </button>
      </div>
    </app-modal>
  `,
})
export class ProjectFormDialog implements OnInit {
  private readonly projects = inject(ProjectsService);

  /** The project to edit; none to create a new one. */
  readonly project = input<Project | null>(null);
  readonly saved = output<Project>();
  readonly closed = output<void>();

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [trimmedMinLength(3), Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(1000)]],
  });
  protected readonly pending = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverErrors = signal<Record<string, string>>({});
  private readonly submitted = signal(false);

  ngOnInit(): void {
    const project = this.project();
    if (project) {
      this.form.setValue({ name: project.name, description: project.description ?? '' });
    }
  }

  protected nameError(): string | null {
    const control = this.form.controls.name;
    if (control.invalid && (control.touched || this.submitted())) {
      return control.hasError('required') ? 'Name must not be empty.' : 'Name must be between 3 and 100 characters.';
    }
    return this.serverErrors()['name'] ?? null;
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
    const { name, description } = this.form.getRawValue();
    const body = { name: name.trim(), description: description.trim() || null };
    const project = this.project();
    const request = project ? this.projects.update(project.id, body) : this.projects.create(body);
    request.subscribe({
      next: (saved) => this.saved.emit(saved),
      error: (error: unknown) => {
        // Keep what was typed, so it can be corrected.
        this.pending.set(false);
        const problem = toApiProblem(error);
        if (problem.code === 'VALIDATION_FAILED') {
          this.serverErrors.set(fieldMessages(problem));
        } else {
          this.error.set(problem.detail ?? 'Could not save the project. Try again.');
        }
      },
    });
  }
}
