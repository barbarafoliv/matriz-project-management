import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { sessionExpiredInterceptor } from './core/auth/session-expired.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // Route parameters, such as :projectId, are passed to components as inputs.
    provideRouter(routes, withComponentInputBinding()),
    // Angular sends the XSRF-TOKEN cookie back in the X-XSRF-TOKEN header on POST, PUT and DELETE.
    provideHttpClient(withInterceptors([sessionExpiredInterceptor])),
  ]
};
