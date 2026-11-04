import { Routes } from '@angular/router';

import { signedInGuard, signedOutGuard } from './core/auth/auth.guards';
import { Home } from './features/home/home';
import { SignIn } from './features/sign-in/sign-in';

export const routes: Routes = [
  { path: 'sign-in', component: SignIn, canActivate: [signedOutGuard], title: 'Sign in · Matriz' },
  { path: '', component: Home, canActivate: [signedInGuard], title: 'Matriz' },
  { path: '**', redirectTo: '' },
];
