import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, map, catchError, switchMap, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { StoreContextService } from './store-context.service';

export interface ApiResponse<T> {
  status: string;
  statusCode: number;
  message: string;
  service: string;
  data: T;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = environment.apiUrl;

  constructor(
    private readonly http: HttpClient,
    private readonly storeContext: StoreContextService,
  ) {}

  getData<T>(path: string, params?: Record<string, string>): Observable<T> {
    return this.http
      .get<ApiResponse<T>>(`${this.baseUrl}${path}`, { params })
      .pipe(map((r) => r.data));
  }

  postData<T>(path: string, body: unknown, idempotencyKey?: string): Observable<T> {
    let headers = new HttpHeaders();
    if (idempotencyKey) {
      headers = headers.set('Idempotency-Key', idempotencyKey);
    }
    return this.http
      .post<ApiResponse<T>>(`${this.baseUrl}${path}`, body, { headers })
      .pipe(map((r) => r.data));
  }

  putData<T>(path: string, body: unknown): Observable<T> {
    return this.http
      .put<ApiResponse<T>>(`${this.baseUrl}${path}`, body)
      .pipe(map((r) => r.data));
  }

  patchData<T>(path: string, body: unknown): Observable<T> {
    return this.http
      .patch<ApiResponse<T>>(`${this.baseUrl}${path}`, body)
      .pipe(map((r) => r.data));
  }

  deleteData<T>(path: string): Observable<T> {
    return this.http
      .delete<ApiResponse<T>>(`${this.baseUrl}${path}`)
      .pipe(map((r) => r.data));
  }

  getTables(storeId: string): Observable<any[]> {
    return this.getData<any[]>(`/api/v1/stores/${storeId}/tables`).pipe(
      catchError(() => this.getData<any[]>('/api/v1/floor-plan/tables', { storeId })),
    );
  }

  getZones(storeId: string): Observable<any[]> {
    return this.getData<any[]>(`/api/v1/stores/${storeId}/zones`).pipe(
      catchError(() => this.getData<any[]>('/api/v1/floor-plan/zones', { storeId })),
    );
  }

  createZone(storeId: string, body: { name: string; displayOrder?: number }): Observable<any> {
    return this.postData(`/api/v1/stores/${storeId}/zones`, body);
  }

  createTable(storeId: string, body: unknown): Observable<any> {
    return this.postData(`/api/v1/stores/${storeId}/tables`, body);
  }

  updateTable(storeId: string, tableId: string, body: unknown): Observable<any> {
    return this.patchData(`/api/v1/stores/${storeId}/tables/${tableId}`, body);
  }

  deleteTable(storeId: string, tableId: string): Observable<any> {
    return this.deleteData(`/api/v1/stores/${storeId}/tables/${tableId}`);
  }

  getProducts(organizationId?: string, categoryId?: string): Observable<any[]> {
    const params: Record<string, string> = {};
    if (organizationId) params['organizationId'] = organizationId;
    if (categoryId) params['categoryId'] = categoryId;
    return this.getData<any[]>('/api/v1/products', Object.keys(params).length ? params : undefined);
  }

  createCategory(body: { name: string; displayOrder?: number }): Observable<any> {
    return this.postData('/api/v1/categories', body);
  }

  updateCategory(id: string, body: unknown): Observable<any> {
    return this.putData(`/api/v1/categories/${id}`, body);
  }

  deleteCategory(id: string): Observable<any> {
    return this.deleteData(`/api/v1/categories/${id}`);
  }

  createProduct(body: unknown): Observable<any> {
    return this.postData('/api/v1/products', body);
  }

  updateProduct(id: string, body: unknown): Observable<any> {
    return this.putData(`/api/v1/products/${id}`, body);
  }

  deleteProduct(id: string): Observable<any> {
    return this.deleteData(`/api/v1/products/${id}`);
  }

  listMembers(): Observable<any[]> {
    return this.getData<any[]>('/api/v1/memberships');
  }

  inviteMember(body: { email: string; role: string; storeId?: string; storeIds?: string[] }): Observable<any> {
    return this.postData('/api/v1/memberships/invite', body);
  }

  updateMembership(membershipId: string, body: unknown): Observable<any> {
    return this.patchData(`/api/v1/memberships/${membershipId}`, body);
  }

  setPin(userId: string, pin: string): Observable<any> {
    return this.postData('/api/v1/auth/set-pin', { userId, pin });
  }

  getResolvedProducts(storeId: string, organizationId: string): Observable<any[]> {
    return this.getData<any[]>(`/api/v1/stores/${storeId}/products`, { organizationId }).pipe(
      catchError(() =>
        this.getData<any[]>(`/api/v1/catalog/stores/${storeId}/products`, { organizationId }),
      ),
    );
  }

  getCategories(organizationId?: string): Observable<any[]> {
    const params = organizationId ? { organizationId } : undefined;
    return this.getData<any[]>('/api/v1/categories', params as Record<string, string> | undefined);
  }

  createOrder(payload: unknown, idempotencyKey: string): Observable<any> {
    return this.postData<any>('/api/v1/orders', payload, idempotencyKey).pipe(
      switchMap((order) => {
        if ((payload as any)?.sendToKitchen && order?.id) {
          return this.sendToKitchen(order.id).pipe(
            map((updated) => updated || { ...order, status: 'SENT_TO_KITCHEN' }),
            catchError(() => of({ ...order, status: 'SENT_TO_KITCHEN' })),
          );
        }
        return of(order);
      }),
    );
  }

  sendToKitchen(orderId: string): Observable<any> {
    return this.postData<any>(`/api/v1/orders/${orderId}/send-to-kitchen`, {}).pipe(
      catchError(() =>
        this.patchData<any>(`/api/v1/kitchen/orders/${orderId}/status`, { status: 'SENT_TO_KITCHEN' }),
      ),
    );
  }

  deliverOrder(orderId: string): Observable<any> {
    return this.postData<any>(`/api/v1/orders/${orderId}/deliver`, {}).pipe(
      catchError(() =>
        this.patchData<any>(`/api/v1/kitchen/orders/${orderId}/status`, { status: 'DELIVERED' }),
      ),
    );
  }

  updateOrderTable(orderId: string, tableId: string | null): Observable<any> {
    return this.patchData<any>(`/api/v1/orders/${orderId}/table`, { tableId });
  }

  getOrganization(orgId: string): Observable<any> {
    return this.getData<any>(`/api/v1/organizations/${orgId}`);
  }

  updateOrganization(
    orgId: string,
    body: { name?: string; mobileMoneyLabel?: string; logoUrl?: string },
  ): Observable<any> {
    return this.patchData<any>(`/api/v1/organizations/${orgId}`, body);
  }

  markPaid(orderId: string, body: unknown, idempotencyKey: string): Observable<any> {
    return this.postData<any>(`/api/v1/orders/${orderId}/payment/mark-paid`, body, idempotencyKey).pipe(
      catchError(() =>
        this.postData<any>('/api/v1/payments', {
          organizationId: this.storeContext.organizationId,
          storeId: this.storeContext.storeId,
          orderId,
          cashierUserId: this.storeContext.userId,
          paymentMethod: (body as any)?.paymentMethod || 'CASH',
          amount: (body as any)?.amount,
          amountTendered: (body as any)?.amountTendered,
        }, idempotencyKey).pipe(
          map(() => ({ id: orderId, paymentStatus: 'PAID', status: 'CLOSED' })),
        ),
      ),
    );
  }

  setStorePriceOverride(storeId: string, productId: string, overridePrice: number): Observable<any> {
    return this.putData(`/api/v1/stores/${storeId}/products/${productId}`, {
      overridePrice,
      priceOverride: overridePrice,
      available: true,
      organizationId: this.storeContext.organizationId,
    }).pipe(
      catchError(() =>
        this.postData(`/api/v1/catalog/stores/${storeId}/overrides`, {
          organizationId: this.storeContext.organizationId,
          productId,
          overridePrice,
          available: true,
        }),
      ),
    );
  }

  requireStoreId(): string {
    const storeId = this.storeContext.storeId;
    if (!storeId) {
      throw new Error('Store context missing');
    }
    return storeId;
  }

  requireOrgId(): string {
    const orgId = this.storeContext.organizationId;
    if (!orgId) {
      throw new Error('Organization context missing');
    }
    return orgId;
  }
}
