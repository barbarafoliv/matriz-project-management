import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Member } from '../../core/projects/project';
import { Task } from '../../core/tasks/task';
import { AssigneesPanel } from './assignees-panel';
import { aTask } from '../../testing/task-fixtures';

const members: Member[] = [
  { userId: 1, name: 'Alice Example', email: 'alice@example.com', role: 'PROJECT_MANAGER' },
  { userId: 2, name: 'Bob Example', email: 'bob@example.com', role: 'MEMBER' },
  { userId: 3, name: 'Carol Example', email: 'carol@example.com', role: 'MEMBER' },
];

describe('AssigneesPanel', () => {
  let fixture: ComponentFixture<AssigneesPanel>;
  let http: HttpTestingController;
  let page: HTMLElement;
  let changed: ReturnType<typeof vi.fn<(task: Task) => void>>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssigneesPanel],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AssigneesPanel);
    fixture.componentRef.setInput('task', aTask({ id: 3, assignees: [{ id: 2, name: 'Bob Example' }] }));
    fixture.componentRef.setInput('members', members);
    changed = vi.fn<(task: Task) => void>();
    fixture.componentInstance.changed.subscribe(changed);
    page = fixture.nativeElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('offers only project members who are not assigned yet', () => {
    const options = Array.from(page.querySelectorAll('option')).map((option) => option.textContent!.trim());

    expect(options).toEqual(['Choose a member…', 'Alice Example (PM)', 'Carol Example']);
    expect(page.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(true);
  });

  it('assigns the chosen member and reports the task from the API', async () => {
    const picker = page.querySelector<HTMLSelectElement>('#assignee-picker')!;
    picker.selectedIndex = 2;
    picker.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    page.querySelector('form')!.dispatchEvent(new Event('submit'));

    const request = http.expectOne('/api/projects/7/tasks/3/assignees');
    expect(request.request.body).toEqual({ userId: 3 });
    const updated = aTask({ id: 3, assignees: [{ id: 2, name: 'Bob Example' }, { id: 3, name: 'Carol Example' }] });
    request.flush(updated);

    expect(changed).toHaveBeenCalledWith(updated);
  });

  it('removes an assignment', () => {
    page.querySelector<HTMLButtonElement>('[aria-label="Remove Bob Example"]')!.click();

    const request = http.expectOne('/api/projects/7/tasks/3/assignees/2');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });

    expect(changed).toHaveBeenCalledWith(expect.objectContaining({ assignees: [] }));
  });

  it('shows why the API refused the assignment', async () => {
    const picker = page.querySelector<HTMLSelectElement>('#assignee-picker')!;
    picker.selectedIndex = 1;
    picker.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    page.querySelector('form')!.dispatchEvent(new Event('submit'));
    http.expectOne('/api/projects/7/tasks/3/assignees').flush(
      { code: 'ALREADY_ASSIGNED', detail: 'This person is already assigned to the task.' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    expect(page.querySelector('[role="alert"]')!.textContent).toContain('This person is already assigned to the task.');
    expect(changed).not.toHaveBeenCalled();
  });
});
