import { Routes } from '@angular/router';

import { signedInGuard, signedOutGuard } from './core/auth/auth.guards';
import { Home } from './features/home/home';
import { ProjectDetail } from './features/projects/project-detail';
import { ProjectsPage } from './features/projects/projects-page';
import { SignIn } from './features/sign-in/sign-in';

export const routes: Routes = [
  { path: 'sign-in', component: SignIn, canActivate: [signedOutGuard], title: 'Sign in · Matriz' },
  {
    path: '',
    canActivate: [signedInGuard],
    children: [
      { path: '', component: Home, title: 'Matriz' },
      { path: 'projects', component: ProjectsPage, title: 'Projects · Matriz' },
      { path: 'projects/:projectId', component: ProjectDetail, title: 'Project · Matriz' },
    ],
  },
  { path: '**', redirectTo: '' },
];
