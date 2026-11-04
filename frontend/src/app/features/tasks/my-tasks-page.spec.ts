import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { MyTask } from '../../core/tasks/task';
import { MyTasksPage } from './my-tasks-page';
import { aTask } from '../../testing/task-fixtures';

const myTask = (id: number, title: string, projectId: number, projectName: string, overrides: Partial<MyTask> = {}): MyTask => ({
  ...aTask({ id, title, projectId }),
  project: { id: projectId, name: projectName },
  ...overrides,
});

describe('MyTasksPage', () => {
  let fixture: ComponentFixture<MyTasksPage>;
  let http: HttpTestingController;
  let page: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyTasksPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MyTasksPage);
    page = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('lists my tasks from all my projects, each with its project name', () => {
    http.expectOne('/api/users/me/tasks').flush([
      myTask(1, 'Write the copy', 7, 'Website redesign', { late: true, dueDate: '2026-11-01' }),
      myTask(2, 'Store listing', 8, 'Mobile launch', { status: 'IN_PROGRESS', priority: 'HIGH' }),
    ]);
    fixture.detectChanges();
    const rows = page.querySelectorAll('tbody tr');

    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('Website redesign');
    expect(rows[0].classList).toContain('late');
    expect(rows[0].textContent).toContain('Late');
    expect(rows[0].textContent).toContain('01/11/2026');
    expect(rows[1].textContent).toContain('Mobile launch');
    expect(rows[1].textContent).toContain('In Progress');
  });

  it('opens the project board with the task highlighted', () => {
    http.expectOne('/api/users/me/tasks').flush([myTask(5, 'Write the copy', 7, 'Website redesign')]);
    fixture.detectChanges();

    expect(page.querySelector('tbody a')!.getAttribute('href')).toBe('/projects/7?highlight=5');
  });

  it('says when nothing is assigned to me', () => {
    http.expectOne('/api/users/me/tasks').flush([]);
    fixture.detectChanges();

    expect(page.textContent).toContain('You have no tasks assigned to you right now.');
  });

  it('shows an error with a way to try again', () => {
    http.expectOne('/api/users/me/tasks').flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    page.querySelector<HTMLButtonElement>('.btn-link')!.click();
    http.expectOne('/api/users/me/tasks').flush([]);
    fixture.detectChanges();

    expect(page.textContent).toContain('All clear!');
  });
});
