import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, map, of, tap, throwError } from 'rxjs';

import { API_BASE_URL } from '../api.config';
import { User } from './user';

/**
 * Who is signed in (US-01). The session itself is a cookie managed by the backend; this
 * service only keeps the user returned by the API.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly user = signal<User | null>(null);
  private checked = false;

  /** The signed-in user, or null. */
  readonly currentUser = this.user.asReadonly();

  /** True after a request failed because the session had expired. */
  readonly sessionExpired = signal(false);

  /** True after the signed-in user deleted their account (US-09). */
  readonly accountDeleted = signal(false);

  /**
   * Asks the API who is signed in, once. Also gives the browser the CSRF cookie needed
   * for the first sign-in.
   */
  loadCurrentUser(): Observable<User | null> {
    if (this.checked) {
      return of(this.user());
    }
    return this.http.get<User>(`${API_BASE_URL}/users/me`).pipe(
      tap((user) => this.setUser(user)),
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.setUser(null);
          return of(null);
        }
        return throwError(() => error);
      }),
    );
  }

  signIn(email: string, password: string): Observable<User> {
    return this.http.post<User>(`${API_BASE_URL}/sessions`, { email, password }).pipe(
      tap((user) => {
        this.sessionExpired.set(false);
        this.accountDeleted.set(false);
        this.setUser(user);
      }),
    );
  }

  signOut(): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/sessions/current`).pipe(
      map(() => undefined),
      finalize(() => {
        this.user.set(null);
        // Signing out also removes the CSRF token. Asking the API again on the next page
        // gets a new one, which the next sign-in needs.
        this.checked = false;
      }),
    );
  }

  /**
   * Deletes the signed-in user's account; the API also signs them out (US-09). Fails with
   * `ACCOUNT_MANAGES_PROJECTS` while they manage a project (EC-06).
   */
  deleteAccount(): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/users/me`).pipe(
      tap(() => {
        this.user.set(null);
        this.accountDeleted.set(true);
        // Ask the API again on the next page, for a new CSRF token.
        this.checked = false;
      }),
    );
  }

  /** Called when the API answers 401 to a signed-in user. */
  handleExpiredSession(): void {
    if (this.user() !== null) {
      this.sessionExpired.set(true);
    }
    this.setUser(null);
  }

  private setUser(user: User | null): void {
    this.user.set(user);
    this.checked = true;
  }
}
