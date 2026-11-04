import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);
  protected readonly signingOut = signal(false);

  protected signOut(): void {
    this.signingOut.set(true);
    this.auth.signOut().subscribe({
      complete: () => this.leave(),
      error: () => this.leave(),
    });
  }

  private leave(): void {
    this.signingOut.set(false);
    this.router.navigate(['/sign-in']);
  }
}
