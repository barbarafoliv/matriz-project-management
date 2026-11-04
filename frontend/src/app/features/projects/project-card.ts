import { Component, computed, input } from '@angular/core';

import { Project } from '../../core/projects/project';

/**
 * A project card: name, role, description and task counts per status, each with its label,
 * not colour alone (FR-17, NFR-05, docs/ui/design-decisions.md). Used on the dashboard and
 * the Projects screen, on a link to the project:
 * `<a app-project-card class="proj-card" [routerLink]="..." [project]="project">`.
 */
@Component({
  selector: 'a[app-project-card]',
  host: { '[attr.aria-label]': 'label()' },
  template: `
    <div class="proj-card-hd">
      <span class="proj-name">{{ project().name }}</span>
      @if (project().myRole === 'PROJECT_MANAGER') {
        <span class="badge b-pm">PM</span>
      }
    </div>
    <p class="proj-desc">
      @if (project().description) {
        {{ project().description }}
      } @else {
        <em class="faint">No description</em>
      }
    </p>
    <div class="counts" aria-label="Tasks per status">
      <span class="count">To Do <strong>{{ project().taskCounts.todo }}</strong></span>
      <span class="count">In Progress <strong>{{ project().taskCounts.inProgress }}</strong></span>
      <span class="count">Done <strong>{{ project().taskCounts.done }}</strong></span>
    </div>
  `,
})
export class ProjectCard {
  readonly project = input.required<Project>();

  protected readonly label = computed(() => {
    const project = this.project();
    const counts = project.taskCounts;
    const role = project.myRole === 'PROJECT_MANAGER' ? 'you are project manager' : 'member';
    return `${project.name}, ${role}: ${counts.todo} to do, ${counts.inProgress} in progress, ${counts.done} done`;
  });
}
