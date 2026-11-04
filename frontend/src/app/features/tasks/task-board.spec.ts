import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { aTask } from '../../testing/task-fixtures';
import { TaskBoard } from './task-board';


describe('TaskBoard', () => {
  let fixture: ComponentFixture<TaskBoard>;
  let http: HttpTestingController;
  let page: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskBoard],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TaskBoard);
    fixture.componentRef.setInput('projectId', 7);
    page = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function select(id: string, value: string): void {
    const element = page.querySelector<HTMLSelectElement>(`#${id}`)!;
    element.value = value;
    element.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  it('shows one column per status with each task in its column', () => {
    http.expectOne('/api/projects/7/tasks').flush([
      aTask({ id: 1, title: 'Write copy', status: 'TODO' }),
      aTask({ id: 2, title: 'Build page', status: 'IN_PROGRESS' }),
      aTask({ id: 3, title: 'Audit', status: 'DONE' }),
    ]);
    fixture.detectChanges();

    const columns = Array.from(page.querySelectorAll('.col'));
    expect(columns.map((column) => column.querySelector('.col-hd')!.textContent!.replace(/\d/g, '').trim())).toEqual([
      'To Do',
      'In Progress',
      'Done',
    ]);
    expect(columns[1].textContent).toContain('Build page');
    expect(page.querySelector('.tc')!.getAttribute('href')).toBe('/projects/7/tasks/1');
  });

  it('shows the title, assignees, due date and priority on each card, and marks late tasks', () => {
    http.expectOne('/api/projects/7/tasks').flush([
      aTask({
        title: 'Overdue',
        status: 'IN_PROGRESS',
        priority: 'HIGH',
        dueDate: '2026-11-03',
        late: true,
        assignees: [
          { id: 1, name: 'Alice Example' },
          { id: 2, name: 'Bob Example' },
          { id: 3, name: 'Carol Example' },
          { id: 4, name: 'Dan Example' },
        ],
      }),
    ]);
    fixture.detectChanges();
    const card = page.querySelector('.tc')!;

    expect(card.classList).toContain('late');
    expect(card.textContent).toContain('Late');
    expect(card.textContent).toContain('High');
    expect(card.textContent).toContain('03/11/2026');
    expect(Array.from(card.querySelectorAll('.av')).map((av) => av.textContent!.trim())).toEqual(['AE', 'BE', 'CE', '+1']);
    expect(card.querySelector('.avatars')!.getAttribute('aria-label')).toContain('Dan Example');
  });

  it('asks the API for the filtered tasks and shows only the chosen status column', () => {
    http.expectOne('/api/projects/7/tasks').flush([aTask({})]);
    fixture.detectChanges();

    select('filter-priority', 'HIGH');
    http.expectOne('/api/projects/7/tasks?priority=HIGH').flush([aTask({ priority: 'HIGH' })]);
    select('filter-status', 'IN_PROGRESS');
    http.expectOne('/api/projects/7/tasks?status=IN_PROGRESS&priority=HIGH').flush([
      aTask({ status: 'IN_PROGRESS', priority: 'HIGH' }),
    ]);
    fixture.detectChanges();

    expect(page.querySelectorAll('.col').length).toBe(1);
    expect(page.querySelector('.col-hd')!.textContent).toContain('In Progress');
  });

  it('explains when no task matches the filters, and clears them', () => {
    http.expectOne('/api/projects/7/tasks').flush([aTask({})]);
    select('filter-status', 'DONE');
    http.expectOne('/api/projects/7/tasks?status=DONE').flush([]);
    fixture.detectChanges();
    expect(page.textContent).toContain('No tasks match these filters.');

    page.querySelector<HTMLButtonElement>('.empty .btn')!.click();
    http.expectOne('/api/projects/7/tasks').flush([aTask({})]);
    fixture.detectChanges();
    expect(page.querySelectorAll('.tc').length).toBe(1);
  });

  it('highlights the task coming from My tasks', () => {
    fixture.componentRef.setInput('highlight', 2);
    http.expectOne('/api/projects/7/tasks').flush([aTask({ id: 1 }), aTask({ id: 2, title: 'Mine' })]);
    fixture.detectChanges();

    const highlighted = page.querySelectorAll('.tc.highlight');
    expect(highlighted.length).toBe(1);
    expect(highlighted[0].textContent).toContain('Mine');
  });

  it('explains when the project has no tasks yet', () => {
    http.expectOne('/api/projects/7/tasks').flush([]);
    fixture.detectChanges();

    expect(page.textContent).toContain('No tasks yet');
  });

  it('reloads the board after a task is created, without reloading the page', () => {
    http.expectOne('/api/projects/7/tasks').flush([]);
    fixture.componentInstance['onCreated']();
    http.expectOne('/api/projects/7/tasks').flush([aTask({ title: 'New one' })]);
    fixture.detectChanges();

    expect(page.textContent).toContain('New one');
  });
});
