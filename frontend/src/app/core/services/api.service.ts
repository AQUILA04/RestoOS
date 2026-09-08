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

  markPaid(orderId: string, body: unknown, idempotencyKey: string): Observable<any> {
    return this.postData<any>(`/api/v1/orders/${orderId}/payment/mark-paid`, body, idempotencyKey).pipe(
      catchError(() =>
        this.postData<any>('/api/v1/payments', {
          organizationId: this.storeContext.organizationId,
          storeId: this.storeContext.storeId,
          orderId,
          cashierUserId: this.storeContext.userId,
          paymentMethod: 'CASH',
          amount: (body as any)?.amount,
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
