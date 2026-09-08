import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private messageSubject = new Subject<any>();

  public connect(storeId: string, topic: string): void {
    console.log(`[WebSocket] Connecting to store topic: /topic/store/${storeId}/${topic}`);
  }

  public getMessages(): Observable<any> {
    return this.messageSubject.asObservable();
  }

  public disconnect(): void {
    console.log('[WebSocket] Disconnected');
  }
}
