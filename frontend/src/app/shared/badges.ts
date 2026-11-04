import { Component, computed, input } from '@angular/core';

import { Priority, TaskStatus, priorityLabel, statusLabel } from '../core/tasks/task';

const STATUS_CLASSES: Record<TaskStatus, string> = { TODO: 'b-todo', IN_PROGRESS: 'b-prog', DONE: 'b-done' };

/** A task's priority, as a labelled badge (docs/ui/design-decisions.md, `Badge`). */
@Component({
  selector: 'app-priority-badge',
  template: `<span [class]="'badge b-' + priority().toLowerCase()">{{ label() }}</span>`,
})
export class PriorityBadge {
  readonly priority = input.required<Priority>();
  protected readonly label = computed(() => priorityLabel(this.priority()));
}

/** A task's status, as a labelled badge (docs/ui/design-decisions.md, `Badge`). */
@Component({
  selector: 'app-status-badge',
  template: `<span [class]="'badge ' + statusClass()">{{ label() }}</span>`,
})
export class StatusBadge {
  readonly status = input.required<TaskStatus>();
  protected readonly label = computed(() => statusLabel(this.status()));
  protected readonly statusClass = computed(() => STATUS_CLASSES[this.status()]);
}
