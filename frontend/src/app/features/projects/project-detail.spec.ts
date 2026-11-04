import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { Member, Project } from '../../core/projects/project';
import { ProjectDetail } from './project-detail';

const project = (myRole: Project['myRole']): Project => ({
  id: 7,
  name: 'Website redesign',
  description: 'New layout.',
  myRole,
  taskCounts: { todo: 0, inProgress: 0, done: 0 },
  createdAt: '',
  updatedAt: '',
});

const members: Member[] = [
  { userId: 1, name: 'Alice Example', email: 'alice@example.com', role: 'PROJECT_MANAGER' },
  { userId: 2, name: 'Bob Example', email: 'bob@example.com', role: 'MEMBER' },
];

describe('ProjectDetail', () => {
  let fixture: ComponentFixture<ProjectDetail>;
  let http: HttpTestingController;
  let page: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectDetail],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ProjectDetail);
    fixture.componentRef.setInput('projectId', '7');
    page = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function load(role: Project['myRole']): void {
    http.expectOne('/api/projects/7').flush(project(role));
    http.expectOne('/api/projects/7/members').flush(members);
    fixture.detectChanges();
    // The task board loads the project's tasks.
    http.expectOne('/api/projects/7/tasks').flush([]);
    fixture.detectChanges();
  }

  function buttons(): string[] {
    return Array.from(page.querySelectorAll('button')).map((button) => button.textContent!.trim());
  }

  it('shows the project and its members', () => {
    load('MEMBER');

    expect(page.querySelector('h1')?.textContent).toContain('Website redesign');
    expect(page.textContent).toContain('New layout.');
    expect(page.querySelectorAll('.members .av').length).toBe(2);
  });

  it('shows Edit, Delete and Manage Members only to the project manager', () => {
    load('MEMBER');
    expect(buttons()).not.toContain('Edit');
    expect(buttons()).not.toContain('Manage Members');

    fixture.componentInstance['load']();
    fixture.detectChanges();
    load('PROJECT_MANAGER');
    expect(buttons()).toEqual(expect.arrayContaining(['Edit', 'Delete', 'Manage Members']));
  });

  it('says the project was not found when the API answers 404', () => {
    http.expectOne('/api/projects/7').flush({ code: 'PROJECT_NOT_FOUND' }, { status: 404, statusText: 'Not Found' });
    http.expectOne('/api/projects/7/members');
    fixture.detectChanges();

    expect(page.textContent).toContain('Project not found.');
  });

  it('asks for confirmation before deleting, then goes back to the projects', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    load('PROJECT_MANAGER');
    page.querySelector<HTMLButtonElement>('.actions .btn-danger')!.click();
    fixture.detectChanges();

    expect(page.textContent).toContain('Delete project?');
    http.expectNone('/api/projects/7');

    page.querySelector<HTMLButtonElement>('app-confirm-dialog .btn-danger')!.click();
    const request = http.expectOne('/api/projects/7');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });

    expect(navigate).toHaveBeenCalledWith(['/projects']);
  });
});
