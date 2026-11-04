import { AfterViewInit, Component, ElementRef, input, output, viewChild } from '@angular/core';

/**
 * A dialog over the page (docs/ui/design-decisions.md, `Modal`). Closes with the ✕ button,
 * the Escape key or a click outside it. The content goes inside; buttons go in an element
 * with the `modal-footer` attribute.
 */
@Component({
  selector: 'app-modal',
  template: `
    <div class="overlay" (click)="closed.emit()">
      <div
        #dialog
        class="modal"
        role="dialog"
        aria-modal="true"
        [attr.aria-labelledby]="titleId"
        tabindex="-1"
        (click)="$event.stopPropagation()"
        (keydown.escape)="closed.emit()"
      >
        <div class="modal-hd">
          <h2 [id]="titleId">{{ title() }}</h2>
          <button class="btn btn-ghost btn-sm" type="button" aria-label="Close" (click)="closed.emit()">✕</button>
        </div>
        <div class="modal-bd"><ng-content /></div>
        <div class="modal-ft"><ng-content select="[modal-footer]" /></div>
      </div>
    </div>
  `,
})
export class Modal implements AfterViewInit {
  private static nextId = 0;

  readonly title = input.required<string>();
  readonly closed = output<void>();

  protected readonly titleId = `modal-title-${Modal.nextId++}`;
  private readonly dialog = viewChild.required<ElementRef<HTMLElement>>('dialog');

  ngAfterViewInit(): void {
    const element = this.dialog().nativeElement;
    const first = element.querySelector<HTMLElement>('.modal-bd input, .modal-bd textarea, .modal-bd button');
    (first ?? element).focus();
  }
}
