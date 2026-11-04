import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthService } from './auth.service';

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  const alice = { id: 1, name: 'Alice Example', email: 'alice@example.com' };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the signed-in user once', () => {
    let result: unknown;
    auth.loadCurrentUser().subscribe((user) => (result = user));
    http.expectOne('/api/users/me').flush(alice);

    expect(result).toEqual(alice);
    expect(auth.currentUser()).toEqual(alice);

    auth.loadCurrentUser().subscribe();
    http.expectNone('/api/users/me');
  });

  it('treats 401 as not signed in', () => {
    let result: unknown = 'not set';
    auth.loadCurrentUser().subscribe((user) => (result = user));
    http.expectOne('/api/users/me').flush({ code: 'UNAUTHENTICATED' }, { status: 401, statusText: 'Unauthorized' });

    expect(result).toBeNull();
    expect(auth.currentUser()).toBeNull();
  });

  it('signs in and keeps the user', () => {
    auth.signIn('alice@example.com', 'secret').subscribe();
    const request = http.expectOne('/api/sessions');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'alice@example.com', password: 'secret' });
    request.flush(alice);

    expect(auth.currentUser()).toEqual(alice);
  });

  it('forgets the user when signing out', () => {
    auth.signIn('alice@example.com', 'secret').subscribe();
    http.expectOne('/api/sessions').flush(alice);

    auth.signOut().subscribe();
    const request = http.expectOne('/api/sessions/current');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });

    expect(auth.currentUser()).toBeNull();
  });

  it('asks the API again after signing out, to get a new CSRF token for the next sign-in', () => {
    auth.signIn('alice@example.com', 'secret').subscribe();
    http.expectOne('/api/sessions').flush(alice);
    auth.signOut().subscribe();
    http.expectOne('/api/sessions/current').flush(null, { status: 204, statusText: 'No Content' });

    auth.loadCurrentUser().subscribe();
    http.expectOne('/api/users/me').flush({}, { status: 401, statusText: 'Unauthorized' });
  });

  it('signs the user out after deleting their account, and asks the API again on the next page', () => {
    auth.signIn('alice@example.com', 'secret').subscribe();
    http.expectOne('/api/sessions').flush(alice);

    auth.deleteAccount().subscribe();
    const request = http.expectOne('/api/users/me');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });

    expect(auth.currentUser()).toBeNull();
    expect(auth.accountDeleted()).toBe(true);
    auth.loadCurrentUser().subscribe();
    http.expectOne('/api/users/me').flush({}, { status: 401, statusText: 'Unauthorized' });
  });

  it('reports an expired session only if someone was signed in', () => {
    auth.handleExpiredSession();
    expect(auth.sessionExpired()).toBe(false);

    auth.signIn('alice@example.com', 'secret').subscribe();
    http.expectOne('/api/sessions').flush(alice);
    auth.handleExpiredSession();

    expect(auth.sessionExpired()).toBe(true);
    expect(auth.currentUser()).toBeNull();
  });
});
