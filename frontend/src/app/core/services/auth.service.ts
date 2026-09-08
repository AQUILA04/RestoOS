import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { StoreContextService } from './store-context.service';
import { ApiResponse } from './api.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(
    private readonly http: HttpClient,
    private readonly storeContext: StoreContextService,
  ) {}

  get accessToken(): string | null {
    return localStorage.getItem('access_token');
  }

  pinLogin(userId: string, pin: string): Observable<{
    accessToken?: string;
    userId: string;
    organizationId?: string;
    storeId?: string;
    roles?: string[];
    authenticated?: boolean;
  }> {
    const storeId = this.storeContext.storeId || localStorage.getItem('store_id') || '';
    const organizationId =
      this.storeContext.organizationId || localStorage.getItem('organization_id') || '';

    return this.http
      .post<ApiResponse<any>>(`${environment.apiUrl}/api/v1/auth/pin-login`, {
        userId,
        pin,
        storeId: storeId || undefined,
        organizationId: organizationId || undefined,
      })
      .pipe(
        map((r) => r.data),
        tap((data) => {
          if (data && data.authenticated === false) {
            throw new Error('PIN invalide');
          }
          if (!data?.accessToken && !data?.token) {
            throw new Error('PIN invalide');
          }
          const token = data.accessToken || data.token;
          this.storeContext.setSession({
            accessToken: token,
            userId: data?.userId || userId,
            organizationId: data?.organizationId || organizationId,
            storeId: data?.storeId || storeId,
            roles: data?.roles || [],
          });
        }),
      );
  }

  activate(token: string): Observable<{ activated: boolean; userId?: string; email?: string }> {
    return this.http
      .get<ApiResponse<any>>(`${environment.apiUrl}/api/v1/auth/activate`, {
        params: { token },
      })
      .pipe(map((r) => r.data));
  }

  setOidcToken(token: string): void {
    localStorage.setItem('access_token', token);
  }
}
