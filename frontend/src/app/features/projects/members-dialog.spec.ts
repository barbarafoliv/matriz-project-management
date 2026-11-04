import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Member } from '../../core/projects/project';
import { MembersDialog } from './members-dialog';

const members: Member[] = [
  { userId: 1, name: 'Alice Example', email: 'alice@example.com', role: 'PROJECT_MANAGER' },
  { userId: 2, name: 'Bob Example', email: 'bob@example.com', role: 'MEMBER' },
];

describe('MembersDialog', () => {
  let fixture: ComponentFixture<MembersDialog>;
  let http: HttpTestingController;
  let page: HTMLElement;
  let changed: ReturnType<typeof vi.fn<() => void>>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MembersDialog],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MembersDialog);
    fixture.componentRef.setInput('projectId', 7);
    fixture.componentRef.setInput('members', members);
    changed = vi.fn<() => void>();
    fixture.componentInstance.changed.subscribe(changed);
    page = fixture.nativeElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  async function addMember(email: string): Promise<void> {
    const input = page.querySelector<HTMLInputElement>('#member-email')!;
    input.value = email;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    page.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('shows the project manager without a remove button', () => {
    const items = page.querySelectorAll('.member-list li');

    expect(items[0].textContent).toContain('PM');
    expect(items[0].querySelector('button')).toBeNull();
    expect(items[1].querySelector('button')?.textContent).toContain('Remove');
  });

  it('adds a member by email', async () => {
    await addMember('carol@example.com');

    const request = http.expectOne('/api/projects/7/members');
    expect(request.request.body).toEqual({ email: 'carol@example.com' });
    request.flush({ userId: 3, name: 'Carol Example', email: 'carol@example.com', role: 'MEMBER' });
    expect(changed).toHaveBeenCalled();
  });

  it('explains when the email has no account', async () => {
    await addMember('nobody@example.com');
    http.expectOne('/api/projects/7/members').flush(
      { code: 'VALIDATION_FAILED', errors: [{ field: 'email', code: 'USER_NOT_FOUND', message: 'No account found with this email.' }] },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    expect(page.textContent).toContain('No account found with this email.');
    expect(changed).not.toHaveBeenCalled();
  });

  it('explains when the person is already a member', async () => {
    await addMember('bob@example.com');
    http.expectOne('/api/projects/7/members').flush(
      { code: 'ALREADY_MEMBER', detail: 'This person is already a member.' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    expect(page.textContent).toContain('This person is already a member.');
  });

  it('asks for confirmation before removing a member', async () => {
    page.querySelector<HTMLButtonElement>('[aria-label="Remove Bob Example"]')!.click();
    await fixture.whenStable();
    expect(page.textContent).toContain('Remove member?');
    expect(page.textContent).toContain("Bob Example will lose access to this project, and will be removed from the tasks they're assigned to in it.");

    page.querySelector<HTMLButtonElement>('app-confirm-dialog .btn-danger')!.click();
    const request = http.expectOne('/api/projects/7/members/2');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });

    expect(changed).toHaveBeenCalled();
  });
});
