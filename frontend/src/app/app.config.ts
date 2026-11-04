import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { sessionExpiredInterceptor } from './core/auth/session-expired.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // Angular sends the XSRF-TOKEN cookie back in the X-XSRF-TOKEN header on POST, PUT and DELETE.
    provideHttpClient(withInterceptors([sessionExpiredInterceptor])),
  ]
};
