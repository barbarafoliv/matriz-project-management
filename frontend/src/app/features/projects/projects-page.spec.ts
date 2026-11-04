import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Project } from '../../core/projects/project';
import { ProjectsPage } from './projects-page';

const project = (id: number, name: string, myRole: Project['myRole'] = 'MEMBER'): Project => ({
  id,
  name,
  description: null,
  myRole,
  taskCounts: { todo: 2, inProgress: 1, done: 3 },
  createdAt: '2026-11-04T10:00:00Z',
  updatedAt: '2026-11-04T10:00:00Z',
});

describe('ProjectsPage', () => {
  let fixture: ComponentFixture<ProjectsPage>;
  let http: HttpTestingController;
  let page: HTMLElement;

  beforeEach(async () => {
    vi.useFakeTimers();
    await TestBed.configureTestingModule({
      imports: [ProjectsPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ProjectsPage);
    page = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  function render(): void {
    fixture.detectChanges();
  }

  it('lists the projects with their role and task counts', () => {
    expect(page.querySelector('[aria-busy="true"]')).not.toBeNull();
    http.expectOne('/api/projects').flush([project(1, 'Mobile launch'), project(2, 'Website redesign', 'PROJECT_MANAGER')]);
    render();

    const cards = page.querySelectorAll('.proj-card');
    expect(cards.length).toBe(2);
    expect(cards[1].textContent).toContain('PM');
    expect(cards[0].textContent).toContain('To Do 2');
    expect(cards[0].getAttribute('href')).toBe('/projects/1');
  });

  it('searches by name after the user stops typing', () => {
    http.expectOne('/api/projects').flush([project(1, 'Mobile launch')]);
    const input = page.querySelector<HTMLInputElement>('#project-search')!;
    input.value = 'we';
    input.dispatchEvent(new Event('input'));
    input.value = 'web';
    input.dispatchEvent(new Event('input'));
    vi.advanceTimersByTime(300);

    http.expectOne('/api/projects?q=web').flush([]);
    render();
    expect(page.textContent).toContain('No projects match your search.');
  });

  it('explains when there are no projects yet', () => {
    http.expectOne('/api/projects').flush([]);
    render();

    expect(page.textContent).toContain('No projects yet');
  });

  it('shows an error with a way to try again', () => {
    http.expectOne('/api/projects').flush({}, { status: 500, statusText: 'Server Error' });
    render();
    expect(page.querySelector('[role="alert"]')?.textContent).toContain('Could not load projects.');

    page.querySelector<HTMLButtonElement>('.btn-link')!.click();
    http.expectOne('/api/projects').flush([project(1, 'Mobile launch')]);
    render();
    expect(page.querySelectorAll('.proj-card').length).toBe(1);
  });
});
