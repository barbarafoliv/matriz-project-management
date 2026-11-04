import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

/** Home screen after sign-in. Placeholder until the dashboard (US-14, Increment 7). */
@Component({
  selector: 'app-home',
  imports: [RouterLink],
  template: `
    <h1 class="page-title">Hello, {{ auth.currentUser()?.name }}</h1>
    <p class="muted">Your dashboard will appear here. Until then, go to <a routerLink="/projects">your projects</a>.</p>
  `,
})
export class Home {
  protected readonly auth = inject(AuthService);
}
