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
          if (!this.isPublicAppPath(router.url)) {
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

  private isPublicAppPath(url: string): boolean {
    const path = url.split('?')[0] || '/';
    if (path === '/' || path === '') {
      return true;
    }
    return PUBLIC_PATH_PREFIXES.some((prefix) => path === prefix || path.startsWith(`${prefix}/`));
  }
}
