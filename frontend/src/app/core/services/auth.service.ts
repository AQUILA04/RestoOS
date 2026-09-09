import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, from, map, switchMap, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { StoreContextService } from './store-context.service';
import { ApiResponse } from './api.service';

export type SignupPayload = {
  organizationName: string;
  firstName?: string;
  lastName?: string;
  email: string;
  password: string;
  multiStore?: boolean;
};

export type SessionPayload = {
  accessToken?: string;
  needsLogin?: boolean;
  userId?: string;
  organizationId?: string;
  storeId?: string;
  storeCount?: number;
  roles?: string[];
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly pkceKey = 'restoos_pkce_verifier';
  private readonly stateKey = 'restoos_oidc_state';

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
        tap((data: any) => {
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

  signup(payload: SignupPayload): Observable<SessionPayload> {
    return this.http
      .post<ApiResponse<SessionPayload>>(`${environment.apiUrl}/api/v1/auth/signup`, payload)
      .pipe(
        map((r) => r.data),
        tap((data) => this.applySession(data)),
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

  logout(): void {
    this.storeContext.clear();
  }

  startKeycloakLogin(loginHint?: string): void {
    from(this.buildAuthorizeUrl(loginHint)).subscribe((url) => {
      window.location.assign(url);
    });
  }

  completeKeycloakLogin(code: string): Observable<SessionPayload> {
    const verifier = sessionStorage.getItem(this.pkceKey) || '';
    return this.http
      .post<ApiResponse<SessionPayload>>(`${environment.apiUrl}/api/v1/auth/oidc/callback`, {
        code,
        codeVerifier: verifier,
        redirectUri: `${environment.frontendPublicUrl}/auth/callback`,
      })
      .pipe(
        map((r) => r.data),
        tap((data) => {
          sessionStorage.removeItem(this.pkceKey);
          sessionStorage.removeItem(this.stateKey);
          this.applySession(data);
        }),
      );
  }

  private applySession(data?: SessionPayload | null): void {
    if (!data?.accessToken) {
      return;
    }
    this.storeContext.setSession({
      accessToken: data.accessToken,
      userId: data.userId || '',
      organizationId: data.organizationId || '',
      storeId: data.storeId || '',
      roles: data.roles || ['OWNER'],
    });
  }

  private async buildAuthorizeUrl(loginHint?: string): Promise<string> {
    const verifier = this.randomString(64);
    const challenge = await this.pkceChallenge(verifier);
    const state = this.randomString(24);
    sessionStorage.setItem(this.pkceKey, verifier);
    sessionStorage.setItem(this.stateKey, state);

    const params = new URLSearchParams({
      client_id: environment.keycloakClientId,
      redirect_uri: `${environment.frontendPublicUrl}/auth/callback`,
      response_type: 'code',
      scope: 'openid profile email',
      state,
      code_challenge: challenge,
      code_challenge_method: 'S256',
    });
    if (loginHint) {
      params.set('login_hint', loginHint);
    }
    return `${environment.keycloakUrl}/realms/${environment.keycloakRealm}/protocol/openid-connect/auth?${params}`;
  }

  private randomString(length: number): string {
    const bytes = new Uint8Array(length);
    crypto.getRandomValues(bytes);
    return Array.from(bytes, (b) => ('0' + (b % 36).toString(36)).slice(-2))
      .join('')
      .slice(0, length);
  }

  private async pkceChallenge(verifier: string): Promise<string> {
    const data = new TextEncoder().encode(verifier);
    const digest = await crypto.subtle.digest('SHA-256', data);
    return this.base64Url(digest);
  }

  private base64Url(buffer: ArrayBuffer): string {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    bytes.forEach((b) => (binary += String.fromCharCode(b)));
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  }
}
