import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface PendingOrder {
  tempUuid: string;
  payload: any;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class OfflineQueueService {
  private queueSubject = new BehaviorSubject<PendingOrder[]>([]);
  private isOnlineSubject = new BehaviorSubject<boolean>(navigator.onLine);

  constructor() {
    window.addEventListener('online', () => this.isOnlineSubject.next(true));
    window.addEventListener('offline', () => this.isOnlineSubject.next(false));
  }

  public isOnline(): Observable<boolean> {
    return this.isOnlineSubject.asObservable();
  }

  public getQueue(): Observable<PendingOrder[]> {
    return this.queueSubject.asObservable();
  }

  public enqueueOrder(payload: any): PendingOrder {
    const currentQueue = this.queueSubject.value;
    const pending: PendingOrder = {
      tempUuid: crypto.randomUUID(),
      payload,
      createdAt: new Date().toISOString()
    };
    this.queueSubject.next([...currentQueue, pending]);
    return pending;
  }

  public clearQueue(): void {
    this.queueSubject.next([]);
  }
}
