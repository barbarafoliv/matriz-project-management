import { Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { fieldMessages, toApiProblem } from '../../core/api-problem';
import { Member } from '../../core/projects/project';
import { ProjectsService } from '../../core/projects/projects.service';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { Modal } from '../../shared/modal';
import { initials } from '../../shared/initials';

/** Manage Members, for the project manager only (US-03, EC-07). */
@Component({
  selector: 'app-members-dialog',
  imports: [Modal, ConfirmDialog, FormsModule],
  template: `
    <app-modal title="Manage Members" (closed)="closed.emit()">
      <form class="field" (ngSubmit)="add()">
        <label class="label" for="member-email">Add member by email</label>
        <div class="row">
          <input
            class="input grow"
            id="member-email"
            type="email"
            name="email"
            placeholder="colleague@example.com"
            [(ngModel)]="email"
            [attr.aria-invalid]="!!addError()"
            aria-describedby="member-email-hint"
          />
          <button class="btn btn-primary btn-sm" type="submit" [disabled]="adding()">Add</button>
        </div>
        @if (addError(); as message) {
          <span class="err-msg" role="alert">{{ message }}</span>
        } @else {
          <span class="hint" id="member-email-hint">The person must already have a Matriz account.</span>
        }
      </form>
      <hr />
      <ul class="member-list" aria-label="Current members">
        @for (member of members(); track member.userId) {
          <li>
            <span class="av" [class.av-pm]="member.role === 'PROJECT_MANAGER'" aria-hidden="true">{{ initials(member.name) }}</span>
            <span class="grow">{{ member.name }} <span class="muted">{{ member.email }}</span></span>
            @if (member.role === 'PROJECT_MANAGER') {
              <span class="badge b-pm">PM</span>
            } @else {
              <button class="btn btn-danger btn-sm" type="button" [attr.aria-label]="'Remove ' + member.name" (click)="removing.set(member)">
                Remove
              </button>
            }
          </li>
        }
      </ul>
      <div modal-footer>
        <button class="btn" type="button" (click)="closed.emit()">Close</button>
      </div>
    </app-modal>

    @if (removing(); as member) {
      <app-confirm-dialog
        title="Remove member?"
        [message]="member.name + ' will lose access to this project, and will be removed from the tasks they\\'re assigned to in it.'"
        confirmLabel="Remove"
        [pending]="removePending()"
        [error]="removeError()"
        (confirmed)="remove(member)"
        (closed)="cancelRemove()"
      />
    }
  `,
  styles: `
    .row { display: flex; gap: 8px; }
    .grow { flex: 1; font-size: 13px; }
    .member-list { list-style: none; display: flex; flex-direction: column; gap: 10px; }
    .member-list li { display: flex; align-items: center; gap: 10px; }
  `,
})
export class MembersDialog {
  private readonly projects = inject(ProjectsService);

  readonly projectId = input.required<number>();
  readonly members = input.required<Member[]>();
  /** Members were added or removed; the parent reloads them. */
  readonly changed = output<void>();
  readonly closed = output<void>();

  protected readonly initials = initials;
  protected email = '';
  protected readonly adding = signal(false);
  protected readonly addError = signal<string | null>(null);
  protected readonly removing = signal<Member | null>(null);
  protected readonly removePending = signal(false);
  protected readonly removeError = signal<string | null>(null);

  protected add(): void {
    const email = this.email.trim();
    if (this.adding()) {
      return;
    }
    if (!email) {
      this.addError.set('Email must not be empty.');
      return;
    }
    this.adding.set(true);
    this.addError.set(null);
    this.projects.addMember(this.projectId(), email).subscribe({
      next: () => {
        this.adding.set(false);
        this.email = '';
        this.changed.emit();
      },
      error: (error: unknown) => {
        this.adding.set(false);
        const problem = toApiProblem(error);
        this.addError.set(fieldMessages(problem)['email'] ?? problem.detail ?? 'Could not add the member. Try again.');
      },
    });
  }

  protected remove(member: Member): void {
    this.removePending.set(true);
    this.removeError.set(null);
    this.projects.removeMember(this.projectId(), member.userId).subscribe({
      next: () => {
        this.cancelRemove();
        this.changed.emit();
      },
      error: (error: unknown) => {
        this.removePending.set(false);
        this.removeError.set(toApiProblem(error).detail ?? 'Could not remove the member. Try again.');
      },
    });
  }

  protected cancelRemove(): void {
    this.removing.set(null);
    this.removePending.set(false);
    this.removeError.set(null);
  }
}
