import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthService } from './auth.service';

/** Only signed-in users can open the application's pages (US-01). */
export const signedInGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(AuthService)
    .loadCurrentUser()
    .pipe(map((user) => (user ? true : router.createUrlTree(['/sign-in']))));
};

/** Signed-in users don't need the sign-in page. */
export const signedOutGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(AuthService)
    .loadCurrentUser()
    .pipe(map((user) => (user ? router.createUrlTree(['/']) : true)));
};
