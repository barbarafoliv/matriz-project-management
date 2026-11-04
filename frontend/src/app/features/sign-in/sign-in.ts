import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { toApiProblem } from '../../core/api-problem';
import { AuthService } from '../../core/auth/auth.service';

/** Sign-in screen (US-01, EC-02). */
@Component({
  selector: 'app-sign-in',
  imports: [ReactiveFormsModule],
  templateUrl: './sign-in.html',
  styleUrl: './sign-in.css',
})
export class SignIn {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: ['', [Validators.required, Validators.maxLength(64)]],
  });

  protected readonly pending = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly sessionExpired = this.auth.sessionExpired;

  protected submit(): void {
    if (this.pending()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.pending.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    this.auth.signIn(email.trim(), password).subscribe({
      next: () => this.router.navigate(['/']),
      error: (error: unknown) => {
        // The form keeps what was typed, so the user can correct it.
        this.pending.set(false);
        this.error.set(this.messageFor(error));
      },
    });
  }

  protected showError(field: 'email' | 'password'): boolean {
    const control = this.form.controls[field];
    return control.invalid && control.touched;
  }

  protected emailError(): string {
    return this.form.controls.email.hasError('required')
      ? 'Email must not be empty.'
      : 'Enter a valid email address.';
  }

  protected passwordError(): string {
    return this.form.controls.password.hasError('required')
      ? 'Password must not be empty.'
      : 'Password must be at most 64 characters.';
  }

  private messageFor(error: unknown): string {
    const problem = toApiProblem(error);
    switch (problem.code) {
      case 'INVALID_CREDENTIALS':
      case 'TOO_MANY_SIGN_IN_ATTEMPTS':
        return problem.detail ?? 'Could not sign in.';
      case 'VALIDATION_FAILED':
        return problem.errors?.map((fieldError) => fieldError.message).join(' ') ?? 'Check the fields.';
      default:
        return 'Could not sign in. Try again.';
    }
  }
}
