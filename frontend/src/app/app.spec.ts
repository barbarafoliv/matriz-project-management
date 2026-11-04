import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';
import { AuthService } from './core/auth/auth.service';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('hides the header when nobody is signed in', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.app-header')).toBeNull();
    expect(fixture.nativeElement.querySelector('router-outlet')).not.toBeNull();
  });

  it('shows the signed-in user and a sign-out button', async () => {
    TestBed.inject(AuthService).signIn('alice@example.com', 'secret').subscribe();
    TestBed.inject(HttpTestingController)
      .expectOne('/api/sessions')
      .flush({ id: 1, name: 'Alice Example', email: 'alice@example.com' });

    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const header: HTMLElement = fixture.nativeElement.querySelector('.app-header');

    expect(header.textContent).toContain('Alice Example');
    expect(header.textContent).toContain('Sign out');
    expect(header.textContent).toContain('Delete account');
  });
});
