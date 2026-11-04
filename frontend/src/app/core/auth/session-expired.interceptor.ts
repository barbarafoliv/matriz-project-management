import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { API_BASE_URL } from '../api.config';
import { AuthService } from './auth.service';

/**
 * Requests whose 401 is handled by the caller, not treated as an expired session: signing
 * in (wrong credentials) and checking who is signed in. Other requests, such as deleting
 * the account, do mean the session expired.
 */
const OWN_401_HANDLING = [`POST ${API_BASE_URL}/sessions`, `GET ${API_BASE_URL}/users/me`];

/**
 * When the API answers 401, the session is no longer valid: send the user to the
 * sign-in screen, which explains that the session expired (US-01).
 */
export const sessionExpiredInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !OWN_401_HANDLING.includes(`${request.method} ${request.url}`)
      ) {
        auth.handleExpiredSession();
        router.navigate(['/sign-in']);
      }
      return throwError(() => error);
    }),
  );
};
