import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { Project } from '../../core/projects/project';
import { Dashboard } from './dashboard';

const project = (id: number, name: string, counts: Project['taskCounts'], myRole: Project['myRole'] = 'MEMBER'): Project => ({
  id,
  name,
  description: null,
  myRole,
  taskCounts: counts,
  createdAt: '',
  updatedAt: '',
});

const counts = (element: Element): string[] =>
  Array.from(element.querySelectorAll('.count')).map((count) => count.textContent!.trim().replace(/\s+/g, ' '));

describe('Dashboard', () => {
  let fixture: ComponentFixture<Dashboard>;
  let http: HttpTestingController;
  let page: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthService).signIn('alice@example.com', 'secret').subscribe();
    http.expectOne('/api/sessions').flush({ id: 1, name: 'Alice Example', email: 'alice@example.com' });
    fixture = TestBed.createComponent(Dashboard);
    page = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it("shows the signed-in user's name and their projects with task counts per status", () => {
    expect(page.querySelector('h1')!.textContent).toContain('Hello, Alice Example');

    http.expectOne('/api/projects').flush([
      project(8, 'Mobile launch', { todo: 1, inProgress: 1, done: 1 }),
      project(7, 'Website redesign', { todo: 2, inProgress: 1, done: 4 }, 'PROJECT_MANAGER'),
    ]);
    fixture.detectChanges();
    const cards = page.querySelectorAll<HTMLAnchorElement>('.proj-card');

    expect(cards.length).toBe(2);
    expect(counts(cards[1])).toEqual(['To Do 2', 'In Progress 1', 'Done 4']);
    expect(cards[1].textContent).toContain('PM');
    expect(cards[1].getAttribute('aria-label')).toBe(
      'Website redesign, you are project manager: 2 to do, 1 in progress, 4 done',
    );
  });

  it('links each project to its detail page', () => {
    http.expectOne('/api/projects').flush([project(7, 'Website redesign', { todo: 0, inProgress: 0, done: 0 })]);
    fixture.detectChanges();

    expect(page.querySelector('.proj-card')!.getAttribute('href')).toBe('/projects/7');
  });

  it('loads the counts again each time it opens, so they follow changes', () => {
    http.expectOne('/api/projects').flush([project(7, 'Website redesign', { todo: 1, inProgress: 0, done: 0 })]);

    const again = TestBed.createComponent(Dashboard);
    again.detectChanges();
    http.expectOne('/api/projects').flush([project(7, 'Website redesign', { todo: 0, inProgress: 0, done: 1 })]);
    again.detectChanges();

    expect(counts(again.nativeElement)).toEqual(['To Do 0', 'In Progress 0', 'Done 1']);
  });

  it('explains when there are no projects yet', () => {
    http.expectOne('/api/projects').flush([]);
    fixture.detectChanges();

    expect(page.textContent).toContain('No projects yet');
    expect(page.querySelector('.empty a')!.getAttribute('href')).toBe('/projects');
  });

  it('shows an error with a way to try again', () => {
    http.expectOne('/api/projects').flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    page.querySelector<HTMLButtonElement>('.btn-link')!.click();
    http.expectOne('/api/projects').flush([]);
    fixture.detectChanges();

    expect(page.textContent).toContain('No projects yet');
  });
});
