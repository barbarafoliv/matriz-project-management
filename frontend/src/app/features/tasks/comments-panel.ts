import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { fieldMessages, toApiProblem } from '../../core/api-problem';
import { Comment, formatDateTime } from '../../core/tasks/task';
import { TasksService } from '../../core/tasks/tasks.service';
import { initials } from '../../shared/initials';

type ListState = 'loading' | 'ready' | 'error';

const MAX_LENGTH = 2000;

/**
 * A task's comments, oldest first, with the author and date, and a form to add one (US-11,
 * EC-10). The new comment is shown from the API's response, which has the author and date
 * set by the server (docs/ui/design-decisions.md).
 */
@Component({
  selector: 'app-comments-panel',
  imports: [FormsModule],
  template: `
    <section class="card panel" aria-labelledby="comments-title">
      <h2 id="comments-title">Comments ({{ comments().length }})</h2>

      @switch (state()) {
        @case ('loading') {
          <div class="skel" style="height: 14px; width: 60%" aria-busy="true" aria-label="Loading comments"></div>
        }
        @case ('error') {
          <div class="alert alert-error" role="alert">
            Could not load the comments. <button class="btn-link" type="button" (click)="load()">Try again</button>
          </div>
        }
        @case ('ready') {
          @if (comments().length) {
            <ul class="comments">
              @for (comment of comments(); track comment.id) {
                <li class="comment">
                  <div class="comment-hd">
                    <span class="av" aria-hidden="true">{{ comment.author ? initials(comment.author.name) : '?' }}</span>
                    <strong>{{ comment.author?.name ?? 'Deleted user' }}</strong>
                    <time [attr.datetime]="comment.createdAt">{{ formatDateTime(comment.createdAt) }}</time>
                  </div>
                  <p>{{ comment.content }}</p>
                </li>
              }
            </ul>
          } @else {
            <p class="hint" role="status">No comments yet.</p>
          }
        }
      }

      <form class="form" (ngSubmit)="add()" novalidate>
        <div class="field">
          <label class="label" for="comment-content">Add a comment</label>
          <textarea
            class="input"
            id="comment-content"
            name="content"
            rows="2"
            [maxlength]="maxLength"
            aria-required="true"
            [attr.aria-invalid]="!!error()"
            aria-describedby="comment-error"
            [(ngModel)]="content"
          ></textarea>
          @if (error(); as message) {
            <span class="err-msg" id="comment-error" role="alert">{{ message }}</span>
          }
        </div>
        <div><button class="btn btn-primary btn-sm" type="submit" [disabled]="pending()">Comment</button></div>
      </form>
    </section>
  `,
})
export class CommentsPanel implements OnInit {
  private readonly tasks = inject(TasksService);

  readonly projectId = input.required<number>();
  readonly taskId = input.required<number>();

  protected readonly initials = initials;
  protected readonly formatDateTime = formatDateTime;
  protected readonly maxLength = MAX_LENGTH;
  protected readonly state = signal<ListState>('loading');
  protected readonly comments = signal<Comment[]>([]);
  protected readonly pending = signal(false);
  protected readonly error = signal<string | null>(null);
  protected content = '';

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.tasks.comments(this.projectId(), this.taskId()).subscribe({
      next: (comments) => {
        this.comments.set(comments);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }

  protected add(): void {
    if (this.pending()) {
      return;
    }
    const text = this.content.trim();
    if (!text) {
      this.error.set('Comment must not be empty.');
      return;
    }
    if (text.length > MAX_LENGTH) {
      this.error.set('Comment must be at most 2000 characters.');
      return;
    }
    this.pending.set(true);
    this.error.set(null);
    this.tasks.addComment(this.projectId(), this.taskId(), text).subscribe({
      next: (comment) => {
        this.pending.set(false);
        this.content = '';
        this.comments.update((comments) => [...comments, comment]);
      },
      error: (error: unknown) => {
        // Keep the text, so it can be sent again.
        this.pending.set(false);
        const problem = toApiProblem(error);
        this.error.set(fieldMessages(problem)['content'] ?? problem.detail ?? 'Could not add the comment. Try again.');
      },
    });
  }
}
