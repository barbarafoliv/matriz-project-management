import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { MyTask, formatDate } from '../../core/tasks/task';
import { TasksService } from '../../core/tasks/tasks.service';
import { PriorityBadge, StatusBadge } from '../../shared/badges';

type PageState = 'loading' | 'ready' | 'error';

/**
 * "My tasks": one flat list of the tasks assigned to me in all my projects, by due date
 * (US-06, US-08, docs/ui/design-decisions.md). A task opens its project's board, with the
 * task highlighted.
 */
@Component({
  selector: 'app-my-tasks-page',
  imports: [RouterLink, PriorityBadge, StatusBadge],
  template: `
    <div class="ph">
      <div>
        <h1 class="page-title">My Tasks</h1>
        <p class="muted">All tasks assigned to you across your projects.</p>
      </div>
    </div>

    @switch (state()) {
      @case ('loading') {
        <div class="card panel" aria-busy="true" aria-label="Loading tasks">
          @for (width of [50, 65, 80]; track width) {
            <div class="skel" style="height: 14px" [style.width.%]="width"></div>
          }
        </div>
      }
      @case ('error') {
        <div class="alert alert-error" role="alert">
          Could not load your tasks. <button class="btn-link" type="button" (click)="load()">Try again</button>
        </div>
      }
      @case ('ready') {
        @if (tasks().length === 0) {
          <div class="empty" role="status">
            <h2>All clear!</h2>
            <p>You have no tasks assigned to you right now.</p>
          </div>
        } @else {
          <div class="card table-wrap">
            <table class="tbl">
              <caption class="visually-hidden">Tasks assigned to me</caption>
              <thead>
                <tr>
                  <th scope="col">Task</th>
                  <th scope="col">Project</th>
                  <th scope="col">Due</th>
                  <th scope="col">Priority</th>
                  <th scope="col">Status</th>
                </tr>
              </thead>
              <tbody>
                @for (task of tasks(); track task.id) {
                  <tr [class.late]="task.late">
                    <td>
                      <a [routerLink]="['/projects', task.project.id]" [queryParams]="{ highlight: task.id }">
                        <strong>{{ task.title }}</strong>
                      </a>
                      @if (task.late) {
                        <span class="badge b-late">Late</span>
                      }
                    </td>
                    <td class="muted">{{ task.project.name }}</td>
                    <td>{{ formatDate(task.dueDate) }}</td>
                    <td><app-priority-badge [priority]="task.priority" /></td>
                    <td><app-status-badge [status]="task.status" /></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      }
    }
  `,
})
export class MyTasksPage implements OnInit {
  private readonly tasksService = inject(TasksService);

  protected readonly formatDate = formatDate;
  protected readonly state = signal<PageState>('loading');
  protected readonly tasks = signal<MyTask[]>([]);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.tasksService.myTasks().subscribe({
      next: (tasks) => {
        this.tasks.set(tasks);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }
}
