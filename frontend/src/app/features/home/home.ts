import { Component, inject } from '@angular/core';

import { AuthService } from '../../core/auth/auth.service';

/** Home screen after sign-in. Placeholder until the dashboard (US-14, Increment 7). */
@Component({
  selector: 'app-home',
  template: `
    <h1 class="page-title">Hello, {{ auth.currentUser()?.name }}</h1>
    <p class="muted">Your projects will appear here.</p>
  `,
})
export class Home {
  protected readonly auth = inject(AuthService);
}
