import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Project } from '../../core/projects/project';
import { ProjectFormDialog } from './project-form-dialog';

describe('ProjectFormDialog', () => {
  let fixture: ComponentFixture<ProjectFormDialog>;
  let http: HttpTestingController;
  let page: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectFormDialog],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function open(project: Project | null = null): Promise<void> {
    fixture = TestBed.createComponent(ProjectFormDialog);
    fixture.componentRef.setInput('project', project);
    page = fixture.nativeElement;
    await fixture.whenStable();
  }

  function type(selector: string, value: string): void {
    const input = page.querySelector<HTMLInputElement | HTMLTextAreaElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  async function submit(): Promise<void> {
    page.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('rejects a name made only of spaces without calling the API', async () => {
    await open();
    type('#project-name', '    ');
    await submit();

    expect(page.textContent).toContain('Name must not be empty.');
    http.expectNone('/api/projects');
  });

  it('creates the project with the trimmed name', async () => {
    await open();
    const saved = vi.fn();
    fixture.componentInstance.saved.subscribe(saved);
    type('#project-name', '  Garden plan ');
    type('#project-description', '   ');
    await submit();

    const request = http.expectOne('/api/projects');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ name: 'Garden plan', description: null });
    request.flush({ id: 9, name: 'Garden plan' });
    expect(saved).toHaveBeenCalledWith({ id: 9, name: 'Garden plan' });
  });

  it('shows the API field errors and keeps what was typed', async () => {
    await open();
    type('#project-name', 'Garden plan');
    await submit();
    http.expectOne('/api/projects').flush(
      { code: 'VALIDATION_FAILED', errors: [{ field: 'name', code: 'TOO_LONG', message: 'Name must be between 3 and 100 characters.' }] },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    expect(page.textContent).toContain('Name must be between 3 and 100 characters.');
    expect(page.querySelector<HTMLInputElement>('#project-name')!.value).toBe('Garden plan');
    expect(page.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(false);
  });

  it('edits an existing project', async () => {
    await open({
      id: 3,
      name: 'Old name',
      description: 'Old',
      myRole: 'PROJECT_MANAGER',
      taskCounts: { todo: 0, inProgress: 0, done: 0 },
      createdAt: '',
      updatedAt: '',
    });
    expect(page.querySelector<HTMLInputElement>('#project-name')!.value).toBe('Old name');
    type('#project-name', 'New name');
    await submit();

    const request = http.expectOne('/api/projects/3');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ name: 'New name', description: 'Old' });
    request.flush({});
  });
});
