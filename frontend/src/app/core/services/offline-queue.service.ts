import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, firstValueFrom } from 'rxjs';
import { openDB, IDBPDatabase } from 'idb';
import { ApiService } from './api.service';

export interface PendingOrder {
  tempUuid: string;
  payload: any;
  createdAt: string;
  status: 'pending' | 'syncing' | 'failed';
  lastError?: string;
}

@Injectable({ providedIn: 'root' })
export class OfflineQueueService {
  private dbPromise: Promise<IDBPDatabase>;
  private readonly queueSubject = new BehaviorSubject<PendingOrder[]>([]);
  private readonly isOnlineSubject = new BehaviorSubject<boolean>(
    typeof navigator !== 'undefined' ? navigator.onLine : true,
  );
  private readonly onOnline = () => {
    this.isOnlineSubject.next(true);
    void this.drain();
  };
  private readonly onOffline = () => this.isOnlineSubject.next(false);

  constructor(private readonly api: ApiService) {
    this.dbPromise = openDB('restoos-offline', 1, {
      upgrade(db) {
        if (!db.objectStoreNames.contains('orders')) {
          db.createObjectStore('orders', { keyPath: 'tempUuid' });
        }
      },
    });
    if (typeof window !== 'undefined') {
      window.addEventListener('online', this.onOnline);
      window.addEventListener('offline', this.onOffline);
      void this.refresh();
    }
  }

  isOnline(): Observable<boolean> {
    return this.isOnlineSubject.asObservable();
  }

  getQueue(): Observable<PendingOrder[]> {
    return this.queueSubject.asObservable();
  }

  async enqueueOrder(payload: any): Promise<PendingOrder> {
    const pending: PendingOrder = {
      tempUuid: crypto.randomUUID(),
      payload,
      createdAt: new Date().toISOString(),
      status: 'pending',
    };
    const db = await this.dbPromise;
    await db.put('orders', pending);
    await this.refresh();
    if (navigator.onLine) {
      void this.drain();
    }
    return pending;
  }

  async drain(): Promise<void> {
    if (!navigator.onLine) {
      return;
    }
    const db = await this.dbPromise;
    const all = (await db.getAll('orders')) as PendingOrder[];
    for (const item of all) {
      try {
        item.status = 'syncing';
        await db.put('orders', item);
        const idempotencyKey = item.payload?.idempotencyKey || item.tempUuid;
        await firstValueFrom(this.api.postData<any>('/api/v1/orders', item.payload, idempotencyKey));
        await db.delete('orders', item.tempUuid);
      } catch (e: any) {
        item.status = 'failed';
        item.lastError = e?.message || 'sync failed';
        await db.put('orders', item);
      }
    }
    await this.refresh();
  }

  async clearQueue(): Promise<void> {
    const db = await this.dbPromise;
    await db.clear('orders');
    await this.refresh();
  }

  private async refresh(): Promise<void> {
    const db = await this.dbPromise;
    const all = (await db.getAll('orders')) as PendingOrder[];
    this.queueSubject.next(all);
  }
}
