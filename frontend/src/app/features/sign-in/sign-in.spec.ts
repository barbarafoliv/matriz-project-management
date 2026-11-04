import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { SignIn } from './sign-in';

describe('SignIn', () => {
  let fixture: ComponentFixture<SignIn>;
  let page: HTMLElement;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SignIn],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(SignIn);
    page = fixture.nativeElement;
    await fixture.whenStable();
  });

  function type(id: string, value: string): void {
    const input = page.querySelector<HTMLInputElement>(`#${id}`)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  async function submit(): Promise<void> {
    page.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    await fixture.whenStable();
  }

  it('points out empty fields without calling the API', async () => {
    await submit();

    expect(page.querySelector('#email-error')?.textContent).toContain('Email must not be empty.');
    expect(page.querySelector('#password-error')?.textContent).toContain('Password must not be empty.');
    http.expectNone('/api/sessions');
  });

  it('shows the API message for wrong credentials and keeps what was typed', async () => {
    type('email', 'alice@example.com');
    type('password', 'wrong');
    await submit();

    expect(page.querySelector('button')!.disabled).toBe(true);
    http.expectOne('/api/sessions').flush(
      { code: 'INVALID_CREDENTIALS', detail: 'Email or password is incorrect.' },
      { status: 401, statusText: 'Unauthorized' },
    );
    fixture.detectChanges();
    await fixture.whenStable();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain('Email or password is incorrect.');
    expect(page.querySelector<HTMLInputElement>('#email')!.value).toBe('alice@example.com');
    expect(page.querySelector('button')!.disabled).toBe(false);
  });

  it('shows the lockout message', async () => {
    type('email', 'alice@example.com');
    type('password', 'wrong');
    await submit();
    http.expectOne('/api/sessions').flush(
      { code: 'TOO_MANY_SIGN_IN_ATTEMPTS', detail: 'Too many failed sign-in attempts. Try again in 15 minutes.' },
      { status: 429, statusText: 'Too Many Requests' },
    );
    fixture.detectChanges();
    await fixture.whenStable();

    expect(page.textContent).toContain('Too many failed sign-in attempts. Try again in 15 minutes.');
  });

  it('explains that the session has expired', async () => {
    TestBed.inject(AuthService).sessionExpired.set(true);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(page.textContent).toContain('Your session has expired. Sign in again.');
  });

  it('goes to the home screen after signing in', async () => {
    type('email', 'alice@example.com');
    type('password', 'right');
    await submit();
    http.expectOne('/api/sessions').flush({ id: 1, name: 'Alice Example', email: 'alice@example.com' });

    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/']);
  });
});
