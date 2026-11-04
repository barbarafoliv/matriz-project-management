import { Component, input, output } from '@angular/core';

import { Modal } from './modal';

/**
 * Asks for confirmation before a destructive action (docs/ui/design-decisions.md, Q4).
 * The parent runs the action, and sets `pending` and `error`.
 */
@Component({
  selector: 'app-confirm-dialog',
  imports: [Modal],
  template: `
    <app-modal [title]="title()" (closed)="closed.emit()">
      <p class="confirm-text">{{ message() }}</p>
      @if (error(); as message) {
        <div class="alert alert-error" role="alert">{{ message }}</div>
      }
      <div modal-footer>
        <button class="btn" type="button" (click)="closed.emit()">Cancel</button>
        <button class="btn btn-danger" type="button" [disabled]="pending()" (click)="confirmed.emit()">
          {{ confirmLabel() }}
        </button>
      </div>
    </app-modal>
  `,
  styles: `.confirm-text { font-size: 13px; }`,
})
export class ConfirmDialog {
  readonly title = input.required<string>();
  readonly message = input.required<string>();
  readonly confirmLabel = input('Delete');
  readonly pending = input(false);
  readonly error = input<string | null>(null);
  readonly confirmed = output<void>();
  readonly closed = output<void>();
}
