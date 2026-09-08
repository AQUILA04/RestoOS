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
  private STORAGE_KEY = 'resto_offline_queue';
  private queueSubject = new BehaviorSubject<PendingOrder[]>(this.loadFromStorage());
  private isOnlineSubject = new BehaviorSubject<boolean>(navigator.onLine);

  constructor() {
    window.addEventListener('online', () => this.isOnlineSubject.next(true));
    window.addEventListener('offline', () => this.isOnlineSubject.next(false));
  }

  private loadFromStorage(): PendingOrder[] {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  }

  private saveToStorage(queue: PendingOrder[]): void {
    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(queue));
    } catch (e) {
      console.error('Failed to persist offline queue to localStorage', e);
    }
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
    const updated = [...currentQueue, pending];
    this.queueSubject.next(updated);
    this.saveToStorage(updated);
    return pending;
  }

  public clearQueue(): void {
    this.queueSubject.next([]);
    this.saveToStorage([]);
  }
}
