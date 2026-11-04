import { Component, OnInit, inject, output, signal } from '@angular/core';

import { toApiProblem } from '../../core/api-problem';
import { AuthService } from '../../core/auth/auth.service';
import { ProjectsService } from '../../core/projects/projects.service';
import { Modal } from '../../shared/modal';

type DialogState = 'loading' | 'blocked' | 'ready' | 'error';

/**
 * Delete account (FR-14, US-09). While the user manages projects, the dialog lists them and
 * has no Delete button (EC-06, docs/ui/design-decisions.md).
 */
@Component({
  selector: 'app-delete-account-dialog',
  imports: [Modal],
  template: `
    <app-modal title="Delete account?" (closed)="closed.emit()">
      @switch (state()) {
        @case ('loading') {
          <div class="skel" style="height: 14px; width: 70%" aria-busy="true" aria-label="Checking your projects"></div>
        }
        @case ('error') {
          <div class="alert alert-error" role="alert">
            Could not check your projects. <button class="btn-link" type="button" (click)="check()">Try again</button>
          </div>
        }
        @case ('blocked') {
          <div class="alert alert-error" role="alert">
            You are the project manager of <strong>{{ managed().join(', ') }}</strong>.
            Delete {{ managed().length > 1 ? 'those projects' : 'that project' }} before deleting your account.
          </div>
        }
        @case ('ready') {
          <p class="confirm-text">
            Your name, email, memberships and assignments will be deleted. Your comments will be kept and shown as
            written by "Deleted user". This action cannot be undone.
          </p>
        }
      }
      @if (error(); as message) {
        <div class="alert alert-error" role="alert">{{ message }}</div>
      }
      <div modal-footer>
        <button class="btn" type="button" (click)="closed.emit()">Cancel</button>
        @if (state() === 'ready') {
          <button class="btn btn-danger" type="button" [disabled]="pending()" (click)="deleteAccount()">Delete account</button>
        }
      </div>
    </app-modal>
  `,
  styles: `.confirm-text { font-size: 13px; }`,
})
export class DeleteAccountDialog implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly projects = inject(ProjectsService);

  /** The account was deleted and the user signed out. */
  readonly deleted = output<void>();
  readonly closed = output<void>();

  protected readonly state = signal<DialogState>('loading');
  protected readonly managed = signal<string[]>([]);
  protected readonly pending = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.check();
  }

  /** Finds the projects the user manages: they block the deletion (EC-06). */
  protected check(): void {
    this.state.set('loading');
    this.projects.list().subscribe({
      next: (projects) => {
        const managed = projects.filter((project) => project.myRole === 'PROJECT_MANAGER').map((project) => project.name);
        this.managed.set(managed);
        this.state.set(managed.length ? 'blocked' : 'ready');
      },
      error: () => this.state.set('error'),
    });
  }

  protected deleteAccount(): void {
    if (this.pending()) {
      return;
    }
    this.pending.set(true);
    this.error.set(null);
    this.auth.deleteAccount().subscribe({
      next: () => this.deleted.emit(),
      error: (error: unknown) => {
        this.pending.set(false);
        const problem = toApiProblem(error);
        if (problem.code === 'ACCOUNT_MANAGES_PROJECTS') {
          // A project was created since the dialog opened: show the up-to-date list.
          this.check();
        }
        this.error.set(problem.detail ?? 'Could not delete your account. Try again.');
      },
    });
  }
}
