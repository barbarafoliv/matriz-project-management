import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Comment } from '../../core/tasks/task';
import { CommentsPanel } from './comments-panel';

const comment = (id: number, content: string, author: Comment['author'], createdAt = '2026-11-04T10:05:00Z'): Comment => ({
  id,
  taskId: 3,
  content,
  author,
  createdAt,
});

describe('CommentsPanel', () => {
  let fixture: ComponentFixture<CommentsPanel>;
  let http: HttpTestingController;
  let page: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommentsPanel],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CommentsPanel);
    fixture.componentRef.setInput('projectId', 7);
    fixture.componentRef.setInput('taskId', 3);
    page = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  async function write(text: string): Promise<void> {
    const textarea = page.querySelector<HTMLTextAreaElement>('#comment-content')!;
    textarea.value = text;
    textarea.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    page.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('lists the comments oldest first with their author and date', () => {
    http.expectOne('/api/projects/7/tasks/3/comments').flush([
      comment(1, 'First draft is ready.', { id: 2, name: 'Bob Example' }),
      comment(2, 'Keep the logo on the left?', null),
    ]);
    fixture.detectChanges();
    const items = page.querySelectorAll('.comment');

    expect(page.querySelector('h2')!.textContent).toContain('Comments (2)');
    expect(items[0].textContent).toContain('Bob Example');
    expect(items[0].textContent).toContain('First draft is ready.');
    expect(items[0].querySelector('time')!.textContent).toMatch(/^\d{2}\/11\/2026 \d{2}:05$/);
    expect(items[1].textContent).toContain('Deleted user');
  });

  it('says when there are no comments yet', () => {
    http.expectOne('/api/projects/7/tasks/3/comments').flush([]);
    fixture.detectChanges();

    expect(page.textContent).toContain('No comments yet.');
  });

  it('adds a comment and shows it from the API response, without reloading', async () => {
    http.expectOne('/api/projects/7/tasks/3/comments').flush([]);
    await write('  Looks good to me.  ');

    const request = http.expectOne('/api/projects/7/tasks/3/comments');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ content: 'Looks good to me.' });
    expect(page.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(true);
    request.flush(comment(9, 'Looks good to me.', { id: 1, name: 'Alice Example' }));
    await fixture.whenStable();

    expect(page.querySelector('.comment')!.textContent).toContain('Alice Example');
    expect(page.querySelector<HTMLTextAreaElement>('#comment-content')!.value).toBe('');
  });

  it('rejects a comment made only of spaces without calling the API', async () => {
    http.expectOne('/api/projects/7/tasks/3/comments').flush([]);
    await write('    ');

    expect(page.querySelector('[role="alert"]')!.textContent).toContain('Comment must not be empty.');
    http.expectNone((request) => request.method === 'POST');
  });

  it('counts characters, so 2000 emoji are sent and 2001 are rejected', async () => {
    http.expectOne('/api/projects/7/tasks/3/comments').flush([]);
    await write('😀'.repeat(2001));
    expect(page.querySelector('[role="alert"]')!.textContent).toContain('Comment must be at most 2000 characters.');
    http.expectNone((request) => request.method === 'POST');

    await write('😀'.repeat(2000));
    http.expectOne((request) => request.method === 'POST').flush(comment(9, '😀'.repeat(2000), { id: 1, name: 'Alice Example' }));
  });

  it('keeps the text when the API rejects the comment', async () => {
    http.expectOne('/api/projects/7/tasks/3/comments').flush([]);
    await write('Hello');
    http.expectOne('/api/projects/7/tasks/3/comments').flush(
      { code: 'VALIDATION_FAILED', errors: [{ field: 'content', code: 'TOO_LONG', message: 'Comment must be at most 2000 characters.' }] },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    expect(page.textContent).toContain('Comment must be at most 2000 characters.');
    expect(page.querySelector<HTMLTextAreaElement>('#comment-content')!.value).toBe('Hello');
    expect(page.querySelectorAll('.comment').length).toBe(0);
  });

  it('shows an error with a way to try again', () => {
    http.expectOne('/api/projects/7/tasks/3/comments').flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    page.querySelector<HTMLButtonElement>('.btn-link')!.click();
    http.expectOne('/api/projects/7/tasks/3/comments').flush([]);
    fixture.detectChanges();

    expect(page.textContent).toContain('No comments yet.');
  });
});
