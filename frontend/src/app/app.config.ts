// src/app/app.config.ts
import { ApplicationConfig, provideZoneChangeDetection, PLATFORM_ID, inject } from '@angular/core';
import { provideRouter, withPreloading, PreloadAllModules } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideAppInitializer } from '@angular/core'; // Import provideAppInitializer

import { routes } from './app.routes';
import { AuthService } from './core/services/auth.service';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';

// Renamed and refactored initializer function to be used with provideAppInitializer
// This function will be called by Angular during the application bootstrap.
// It uses inject() to get its dependencies (AuthService, PLATFORM_ID).
export function authInitializer(): Promise<void> {
    const authService = inject(AuthService);
    const platformId = inject(PLATFORM_ID);

    if (isPlatformBrowser(platformId)) {
        console.log('AuthInitializer (via provideAppInitializer): Running in browser. Initializing auth state.');
        // initAuthState should return a Promise for the app to wait for.
        return authService.initAuthState();
    }

    console.log('AuthInitializer (via provideAppInitializer): Running on server. Skipping auth state initialization.');
    // On the server, or if no async action is needed, return a resolved promise.
    return Promise.resolve();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    // Consolidate router provider configuration here
    provideRouter(routes, withPreloading(PreloadAllModules)),

    // Consolidate HttpClient provider configuration here
    provideHttpClient(
      withFetch(), // Important for SSR
      withInterceptors([authInterceptor, errorInterceptor]) // Modern way to provide interceptors
    ),

    AuthService, // Ensure AuthService is provided so it can be injected

    // Use the new provideAppInitializer with the refactored initializer function
    provideAppInitializer(authInitializer),

    // The old way of providing interceptors via HTTP_INTERCEPTORS is no longer needed
    // if they are included in withInterceptors above.
    // {
    //   provide: HTTP_INTERCEPTORS,
    //   useValue: authInterceptor,
    //   multi: true,
    // },
    // {
    //   provide: HTTP_INTERCEPTORS,
    //   useValue: errorInterceptor,
    //   multi: true,
    // },
  ],
};
