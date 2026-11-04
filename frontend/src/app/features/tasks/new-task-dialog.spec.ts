import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NewTaskDialog } from './new-task-dialog';

describe('NewTaskDialog', () => {
  let fixture: ComponentFixture<NewTaskDialog>;
  let http: HttpTestingController;
  let page: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NewTaskDialog],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(NewTaskDialog);
    fixture.componentRef.setInput('projectId', 7);
    page = fixture.nativeElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  function type(selector: string, value: string): void {
    const input = page.querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  async function submit(): Promise<void> {
    page.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('chooses Medium priority by default', () => {
    expect(page.querySelector<HTMLSelectElement>('#task-priority')!.value).toBe('MEDIUM');
  });

  it('rejects a title made only of spaces without calling the API', async () => {
    type('#task-title', '   ');
    await submit();

    expect(page.textContent).toContain('Title must not be empty.');
    http.expectNone('/api/projects/7/tasks');
  });

  it('creates the task with the trimmed title', async () => {
    const created = vi.fn();
    fixture.componentInstance.created.subscribe(created);
    type('#task-title', '  Write the copy ');
    type('#task-due', '2030-01-15');
    await submit();

    const request = http.expectOne('/api/projects/7/tasks');
    expect(request.request.body).toEqual({ title: 'Write the copy', description: null, priority: 'MEDIUM', dueDate: '2030-01-15' });
    expect(page.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(true);
    request.flush({ id: 5 });
    expect(created).toHaveBeenCalled();
  });

  it('shows the API field errors and keeps what was typed', async () => {
    type('#task-title', 'Write the copy');
    await submit();
    http.expectOne('/api/projects/7/tasks').flush(
      { code: 'VALIDATION_FAILED', errors: [{ field: 'description', code: 'TOO_LONG', message: 'Description must be at most 2000 characters.' }] },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    expect(page.textContent).toContain('Description must be at most 2000 characters.');
    expect(page.querySelector<HTMLInputElement>('#task-title')!.value).toBe('Write the copy');
  });
});
