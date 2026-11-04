import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AuthService } from '../../core/auth/auth.service';
import { Project } from '../../core/projects/project';
import { DeleteAccountDialog } from './delete-account-dialog';

const project = (name: string, myRole: Project['myRole']): Project => ({
  id: name.length,
  name,
  description: null,
  myRole,
  taskCounts: { todo: 0, inProgress: 0, done: 0 },
  createdAt: '',
  updatedAt: '',
});

describe('DeleteAccountDialog', () => {
  let fixture: ComponentFixture<DeleteAccountDialog>;
  let http: HttpTestingController;
  let page: HTMLElement;
  let deleted: ReturnType<typeof vi.fn<() => void>>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeleteAccountDialog],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DeleteAccountDialog);
    deleted = vi.fn<() => void>();
    fixture.componentInstance.deleted.subscribe(deleted);
    page = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function deleteButton(): HTMLButtonElement | undefined {
    return Array.from(page.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent!.trim() === 'Delete account');
  }

  it('lists the projects the user manages and offers no Delete button', () => {
    http.expectOne('/api/projects').flush([project('Website redesign', 'PROJECT_MANAGER'), project('Mobile launch', 'MEMBER')]);
    fixture.detectChanges();

    expect(page.textContent).toContain('You are the project manager of Website redesign.');
    expect(page.textContent).toContain('Delete that project before deleting your account.');
    expect(deleteButton()).toBeUndefined();
  });

  it('explains what will be deleted, then deletes the account', () => {
    const auth = TestBed.inject(AuthService);
    http.expectOne('/api/projects').flush([project('Mobile launch', 'MEMBER')]);
    fixture.detectChanges();
    expect(page.textContent).toContain('Your comments will be kept and shown as written by "Deleted user".');

    deleteButton()!.click();
    const request = http.expectOne('/api/users/me');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });

    expect(deleted).toHaveBeenCalled();
    expect(auth.currentUser()).toBeNull();
    expect(auth.accountDeleted()).toBe(true);
  });

  it('shows the API message if the user started managing a project meanwhile', () => {
    http.expectOne('/api/projects').flush([]);
    fixture.detectChanges();

    deleteButton()!.click();
    http.expectOne('/api/users/me').flush(
      { code: 'ACCOUNT_MANAGES_PROJECTS', detail: 'Delete the projects you manage before deleting your account.' },
      { status: 409, statusText: 'Conflict' },
    );
    http.expectOne('/api/projects').flush([project('New project', 'PROJECT_MANAGER')]);
    fixture.detectChanges();

    expect(page.textContent).toContain('Delete the projects you manage before deleting your account.');
    expect(page.textContent).toContain('New project');
    expect(deleted).not.toHaveBeenCalled();
  });
});
