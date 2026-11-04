import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { Observable, firstValueFrom } from 'rxjs';

import { signedInGuard, signedOutGuard } from './auth.guards';

describe('auth guards', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  function run(guard: typeof signedInGuard): Promise<boolean | UrlTree> {
    const result = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    ) as Observable<boolean | UrlTree>;
    return firstValueFrom(result);
  }

  it('sends visitors who are not signed in to the sign-in screen', async () => {
    const result = run(signedInGuard);
    http.expectOne('/api/users/me').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect((await result).toString()).toBe('/sign-in');
  });

  it('lets signed-in users in', async () => {
    const result = run(signedInGuard);
    http.expectOne('/api/users/me').flush({ id: 1, name: 'Alice Example', email: 'alice@example.com' });

    expect(await result).toBe(true);
  });

  it('sends signed-in users away from the sign-in screen', async () => {
    const result = run(signedOutGuard);
    http.expectOne('/api/users/me').flush({ id: 1, name: 'Alice Example', email: 'alice@example.com' });

    expect((await result).toString()).toBe('/');
  });
});
