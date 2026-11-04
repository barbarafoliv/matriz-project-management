import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { AuthService } from './auth.service';
import { sessionExpiredInterceptor } from './session-expired.interceptor';

describe('sessionExpiredInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;
  let auth: AuthService;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([sessionExpiredInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    auth.signIn('alice@example.com', 'secret').subscribe();
    http.expectOne('/api/sessions').flush({ id: 1, name: 'Alice Example', email: 'alice@example.com' });
  });

  it('sends the user to sign in with a message when the session has expired', () => {
    client.get('/api/projects').subscribe({ error: () => undefined });
    http.expectOne('/api/projects').flush({ code: 'UNAUTHENTICATED' }, { status: 401, statusText: 'Unauthorized' });

    expect(auth.sessionExpired()).toBe(true);
    expect(navigate).toHaveBeenCalledWith(['/sign-in']);
  });

  it('treats a 401 when deleting the account as an expired session', () => {
    client.delete('/api/users/me').subscribe({ error: () => undefined });
    http.expectOne('/api/users/me').flush({ code: 'UNAUTHENTICATED' }, { status: 401, statusText: 'Unauthorized' });

    expect(auth.sessionExpired()).toBe(true);
    expect(navigate).toHaveBeenCalledWith(['/sign-in']);
  });

  it('leaves the check of who is signed in to the guards', () => {
    client.get('/api/users/me').subscribe({ error: () => undefined });
    http.expectOne('/api/users/me').flush({ code: 'UNAUTHENTICATED' }, { status: 401, statusText: 'Unauthorized' });

    expect(navigate).not.toHaveBeenCalled();
  });

  it('leaves a failed sign-in to the sign-in screen', () => {
    client.post('/api/sessions', {}).subscribe({ error: () => undefined });
    http.expectOne('/api/sessions').flush({ code: 'INVALID_CREDENTIALS' }, { status: 401, statusText: 'Unauthorized' });

    expect(auth.sessionExpired()).toBe(false);
    expect(navigate).not.toHaveBeenCalled();
  });
});
