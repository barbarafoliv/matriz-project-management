import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { Member, Project } from '../../core/projects/project';
import { Task } from '../../core/tasks/task';
import { aTask } from '../../testing/task-fixtures';
import { TaskDetail } from './task-detail';

const project = (myRole: Project['myRole']): Project => ({
  id: 7,
  name: 'Website redesign',
  description: null,
  myRole,
  taskCounts: { todo: 1, inProgress: 0, done: 0 },
  createdAt: '',
  updatedAt: '',
});

const members: Member[] = [
  { userId: 1, name: 'Alice Example', email: 'alice@example.com', role: 'PROJECT_MANAGER' },
  { userId: 2, name: 'Bob Example', email: 'bob@example.com', role: 'MEMBER' },
];

describe('TaskDetail', () => {
  let fixture: ComponentFixture<TaskDetail>;
  let http: HttpTestingController;
  let page: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskDetail],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TaskDetail);
    fixture.componentRef.setInput('projectId', '7');
    fixture.componentRef.setInput('taskId', '3');
    page = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  async function load(role: Project['myRole'], task: Partial<Task> = {}): Promise<void> {
    http.expectOne('/api/projects/7').flush(project(role));
    http.expectOne('/api/projects/7/tasks/3').flush(aTask({ id: 3, title: 'Write the copy', ...task }));
    http.expectOne('/api/projects/7/members').flush(members);
    fixture.detectChanges();
    await fixture.whenStable();
  }

  function statusButton(label: string): HTMLButtonElement {
    return Array.from(page.querySelectorAll<HTMLButtonElement>('.status-buttons button')).find(
      (button) => button.textContent!.trim() === label,
    )!;
  }

  it('shows the task with its status, priority and assignees', async () => {
    await load('MEMBER', { priority: 'HIGH', assignees: [{ id: 2, name: 'Bob Example' }] });

    expect(page.querySelector('h1')!.textContent).toContain('Write the copy');
    expect(statusButton('To Do').getAttribute('aria-pressed')).toBe('true');
    expect(page.querySelector<HTMLSelectElement>('#task-priority')!.value).toBe('HIGH');
    expect(page.querySelector('.assignees')!.textContent).toContain('Bob Example');
    expect(page.textContent).not.toContain('Delete task');
  });

  it('saves a status change straight away and updates the late label', async () => {
    await load('MEMBER', { status: 'IN_PROGRESS', late: true, dueDate: '2026-11-03' });
    expect(page.textContent).toContain('Late');

    statusButton('Done').click();
    const request = http.expectOne('/api/projects/7/tasks/3/status');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ status: 'DONE' });
    request.flush(aTask({ id: 3, title: 'Write the copy', status: 'DONE', late: false, dueDate: '2026-11-03' }));
    fixture.detectChanges();

    expect(statusButton('Done').getAttribute('aria-pressed')).toBe('true');
    expect(page.querySelector('.badges')!.textContent).not.toContain('Late');
  });

  it('saves edited details', async () => {
    await load('MEMBER');
    const title = page.querySelector<HTMLInputElement>('#task-title')!;
    title.value = 'Write better copy';
    title.dispatchEvent(new Event('input'));
    page.querySelector('form')!.dispatchEvent(new Event('submit'));

    const request = http.expectOne('/api/projects/7/tasks/3');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ title: 'Write better copy', description: null, priority: 'MEDIUM', dueDate: null });
    request.flush(aTask({ id: 3, title: 'Write better copy' }));
    fixture.detectChanges();

    expect(page.querySelector('h1')!.textContent).toContain('Write better copy');
  });

  it('keeps the edits when the API rejects them', async () => {
    await load('MEMBER');
    const title = page.querySelector<HTMLInputElement>('#task-title')!;
    title.value = 'Better copy';
    title.dispatchEvent(new Event('input'));
    page.querySelector('form')!.dispatchEvent(new Event('submit'));
    http.expectOne('/api/projects/7/tasks/3').flush(
      { code: 'VALIDATION_FAILED', errors: [{ field: 'title', code: 'TOO_LONG', message: 'Title must be between 3 and 150 characters.' }] },
      { status: 400, statusText: 'Bad Request' },
    );
    fixture.detectChanges();

    expect(page.textContent).toContain('Title must be between 3 and 150 characters.');
    expect(page.querySelector<HTMLInputElement>('#task-title')!.value).toBe('Better copy');
  });

  it('assigns a member and keeps unsaved edits in the form', async () => {
    await load('MEMBER', { assignees: [{ id: 2, name: 'Bob Example' }] });
    const title = page.querySelector<HTMLInputElement>('#task-title')!;
    title.value = 'Unsaved title';
    title.dispatchEvent(new Event('input'));

    const options = Array.from(page.querySelectorAll('#assignee-picker option')).map((o) => o.textContent!.trim());
    expect(options).toEqual(['Choose a member…', 'Alice Example (PM)']);
    const picker = page.querySelector<HTMLSelectElement>('#assignee-picker')!;
    picker.selectedIndex = 1;
    picker.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    page.querySelector<HTMLFormElement>('app-assignees-panel form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    const request = http.expectOne('/api/projects/7/tasks/3/assignees');
    expect(request.request.body).toEqual({ userId: 1 });
    request.flush(aTask({ id: 3, title: 'Write the copy', assignees: [{ id: 1, name: 'Alice Example' }, { id: 2, name: 'Bob Example' }] }));
    fixture.detectChanges();

    expect(page.querySelector('.assignees')!.textContent).toContain('Alice Example');
    expect(page.textContent).toContain('All project members are assigned.');
    expect(page.querySelector<HTMLInputElement>('#task-title')!.value).toBe('Unsaved title');
  });

  it('lets the project manager delete the task after confirming', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await load('PROJECT_MANAGER');

    Array.from(page.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent!.includes('Delete task'))!.click();
    fixture.detectChanges();
    expect(page.textContent).toContain('Delete task?');
    page.querySelector<HTMLButtonElement>('app-confirm-dialog .btn-danger')!.click();
    const request = http.expectOne('/api/projects/7/tasks/3');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });

    expect(navigate).toHaveBeenCalledWith(['/projects', 7]);
  });

  it('says the task was not found when the API answers 404', () => {
    http.expectOne('/api/projects/7').flush(project('MEMBER'));
    http.expectOne('/api/projects/7/tasks/3').flush({ code: 'TASK_NOT_FOUND' }, { status: 404, statusText: 'Not Found' });
    http.expectOne('/api/projects/7/members');
    fixture.detectChanges();

    expect(page.textContent).toContain('Task not found.');
  });
});
