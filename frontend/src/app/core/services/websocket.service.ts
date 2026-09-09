import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../../environments/environment';
import { StoreContextService } from './store-context.service';

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private client: Client | null = null;
  private readonly messageSubject = new Subject<any>();
  private readonly seenEventIds = new Set<string>();

  constructor(private readonly storeContext: StoreContextService) {}

  connect(storeId: string, topic: 'kitchen' | 'pos'): void {
    this.disconnect();
    const token = localStorage.getItem('access_token') || '';
    this.client = new Client({
      webSocketFactory: () => new SockJS(environment.wsUrl) as any,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 2000,
      onConnect: () => {
        this.client?.subscribe(`/topic/store/${storeId}/${topic}`, (message: IMessage) => {
          try {
            const payload = JSON.parse(message.body);
            const eventId = payload.eventId || payload.orderId + ':' + payload.type + ':' + payload.status;
            if (eventId && this.seenEventIds.has(eventId)) {
              return;
            }
            if (eventId) {
              this.seenEventIds.add(eventId);
            }
            this.messageSubject.next(payload);
          } catch {
            this.messageSubject.next(message.body);
          }
        });
      },
    });
    this.client.activate();
  }

  getMessages(): Observable<any> {
    return this.messageSubject.asObservable();
  }

  disconnect(): void {
    if (this.client) {
      void this.client.deactivate();
      this.client = null;
    }
  }
}
