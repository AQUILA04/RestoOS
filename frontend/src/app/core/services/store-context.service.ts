import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface StoreSession {
  accessToken?: string;
  userId: string;
  organizationId: string;
  storeId: string;
  roles: string[];
  displayName?: string;
}

@Injectable({ providedIn: 'root' })
export class StoreContextService {
  private readonly sessionSubject = new BehaviorSubject<StoreSession | null>(this.readSession());
  readonly session$ = this.sessionSubject.asObservable();

  get session(): StoreSession | null {
    return this.sessionSubject.value;
  }

  get organizationId(): string | null {
    return this.session?.organizationId ?? localStorage.getItem('organization_id') ?? localStorage.getItem('resto_org_id');
  }

  get storeId(): string | null {
    return this.session?.storeId ?? localStorage.getItem('store_id') ?? localStorage.getItem('resto_store_id');
  }

  get userId(): string | null {
    return this.session?.userId ?? localStorage.getItem('user_id') ?? localStorage.getItem('resto_user_id');
  }

  /**
   * True when a bearer token plus user and organization (tenant) IDs are present.
   * Store ID is optional so multi-store owners can reach establishment picker before selecting a site.
   */
  hasTenantSession(): boolean {
    const token = localStorage.getItem('access_token');
    const userId = this.userId?.trim();
    const organizationId = this.organizationId?.trim();
    return !!(token && userId && organizationId);
  }

  /** Station is bound to a tenant site (used before PIN unlock on shared terminals). */
  hasStationBinding(): boolean {
    const organizationId = this.organizationId?.trim();
    const storeId = this.storeId?.trim();
    return !!(organizationId && storeId);
  }

  setSession(session: StoreSession): void {
    if (session.accessToken) {
      localStorage.setItem('access_token', session.accessToken);
    }
    localStorage.setItem('user_id', session.userId);
    localStorage.setItem('organization_id', session.organizationId);
    localStorage.setItem('store_id', session.storeId);
    localStorage.setItem('roles', JSON.stringify(session.roles || []));
    localStorage.setItem('resto_authenticated', 'true');
    this.sessionSubject.next(session);
  }

  setContext(partial: Partial<StoreSession>): void {
    const current = this.session || {
      userId: this.userId || '',
      organizationId: this.organizationId || '',
      storeId: this.storeId || '',
      roles: [],
    };
    this.setSession({ ...current, ...partial });
  }

  /** Drop bearer identity but keep org/store station binding for PIN re-entry. */
  clearAuth(): void {
    const organizationId =
      localStorage.getItem('organization_id') || localStorage.getItem('resto_org_id') || '';
    const storeId = localStorage.getItem('store_id') || localStorage.getItem('resto_store_id') || '';
    ['access_token', 'user_id', 'roles', 'resto_authenticated', 'resto_user_id'].forEach((k) =>
      localStorage.removeItem(k),
    );
    this.sessionSubject.next(
      organizationId || storeId
        ? { userId: '', organizationId, storeId, roles: [] }
        : null,
    );
  }

  clear(): void {
    [
      'access_token',
      'user_id',
      'organization_id',
      'store_id',
      'roles',
      'resto_authenticated',
      'pos_staff',
      'resto_org_id',
      'resto_store_id',
      'resto_user_id',
    ].forEach((k) => localStorage.removeItem(k));
    this.sessionSubject.next(null);
  }

  private readSession(): StoreSession | null {
    const accessToken = localStorage.getItem('access_token') || undefined;
    const userId = localStorage.getItem('user_id');
    const organizationId = localStorage.getItem('organization_id') || localStorage.getItem('resto_org_id');
    const storeId = localStorage.getItem('store_id') || localStorage.getItem('resto_store_id');
    const authenticated = localStorage.getItem('resto_authenticated') === 'true' || !!accessToken;
    if (!authenticated || !userId) {
      return null;
    }
    let roles: string[] = [];
    try {
      roles = JSON.parse(localStorage.getItem('roles') || '[]');
    } catch {
      roles = [];
    }
    return {
      accessToken,
      userId,
      organizationId: organizationId || '',
      storeId: storeId || '',
      roles,
    };
  }
}
