import { Injectable, Injector } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpErrorResponse,
} from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { StoreContextService } from '../services/store-context.service';

const PUBLIC_PATH_PREFIXES = ['/login', '/signup', '/activate', '/auth/callback', '/home'];

/** Station PIN unlock routes: 401s (e.g. staff list without JWT) must not bounce to Keycloak. */
const STATION_PIN_PATH_PREFIXES = ['/pos', '/kds'];

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  constructor(private readonly injector: Injector) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = localStorage.getItem('access_token');
    const authReq = token
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

    return next.handle(authReq).pipe(
      catchError((err: HttpErrorResponse) => {
        if (err.status === 401 && !this.isPublicAuthRequest(req.url)) {
          const storeContext = this.injector.get(StoreContextService);
          const router = this.injector.get(Router);
          storeContext.clearAuth();
          if (!this.shouldSkipLoginRedirect(router.url, storeContext)) {
            void router.navigateByUrl('/login');
          }
        }
        return throwError(() => err);
      }),
    );
  }

  private isPublicAuthRequest(url: string): boolean {
    return (
      url.includes('/auth/pin-login') ||
      url.includes('/auth/signup') ||
      url.includes('/auth/activate') ||
      url.includes('/auth/oidc/callback')
    );
  }

  private shouldSkipLoginRedirect(url: string, storeContext: StoreContextService): boolean {
    if (this.isPublicAppPath(url)) {
      return true;
    }
    const path = url.split('?')[0] || '/';
    const onStationPinRoute = STATION_PIN_PATH_PREFIXES.some(
      (prefix) => path === prefix || path.startsWith(`${prefix}/`),
    );
    // Bound stations stay on PIN lockscreen when unauthenticated API calls 401.
    return onStationPinRoute && storeContext.hasStationBinding();
  }

  private isPublicAppPath(url: string): boolean {
    const path = url.split('?')[0] || '/';
    if (path === '/' || path === '') {
      return true;
    }
    return PUBLIC_PATH_PREFIXES.some((prefix) => path === prefix || path.startsWith(`${prefix}/`));
  }
}
