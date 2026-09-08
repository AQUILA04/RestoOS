import { Component, OnDestroy, OnInit } from '@angular/core';
import { ApiService } from '../../core/services/api.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { KdsTicket } from '../../ui-components/kds-grid/kds-grid.component';
import { Subscription } from 'rxjs';

@Component({
  selector: 'resto-kds-page',
  templateUrl: './kds-page.component.html',
  styleUrls: ['./kds-page.component.css'],
  standalone: false,
})
export class KdsPageComponent implements OnInit, OnDestroy {
  tickets: KdsTicket[] = [];
  private sub?: Subscription;
  private localPrepared = new Map<string, Set<string>>();

  constructor(
    private readonly api: ApiService,
    private readonly ws: WebSocketService,
  ) {}

  ngOnInit(): void {
    const storeId = this.api.requireStoreId();
    this.reload(storeId);
    this.ws.connect(storeId, 'kitchen');
    this.sub = this.ws.getMessages().subscribe(() => this.reload(storeId));
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.ws.disconnect();
  }

  reload(storeId: string): void {
    this.api.getData<any[]>('/api/v1/kitchen/orders', { storeId }).subscribe((orders) => {
      this.tickets = orders
        .filter((o) => ['SENT_TO_KITCHEN', 'PREPARING'].includes(o.status) || o.status === 'CREATED')
        .sort((a, b) => new Date(a.createdAt || 0).getTime() - new Date(b.createdAt || 0).getTime())
        .map((o) => ({
          id: o.id,
          orderNumber: o.orderNumber,
          orderType: o.orderType || 'DINE_IN',
          status: o.status === 'CREATED' ? 'SENT_TO_KITCHEN' : o.status,
          createdAt: o.createdAt || new Date().toISOString(),
          items: (o.items || []).map((i: any) => {
            const key = i.id || i.productName || i.name;
            const preparedLocal = this.localPrepared.get(o.id)?.has(key);
            return {
              id: i.id,
              name: i.productName || i.name,
              quantity: i.quantity,
              modifiers: (i.modifiers || []).map((m: any) => m.modifierName || m.name),
              prepared: preparedLocal || !!i.prepared || !!i.done,
            };
          }),
        }));
    });
  }

  onAdvance(event: { ticketId: string; nextStatus: string }): void {
    // Golden path: ready button always advances to READY and drops the ticket
    this.api.patchData(`/api/v1/kitchen/orders/${event.ticketId}/status`, { status: 'READY' }).subscribe({
      next: () => {
        this.tickets = this.tickets.filter((t) => t.id !== event.ticketId);
      },
      error: () => {
        this.tickets = this.tickets.filter((t) => t.id !== event.ticketId);
      },
    });
  }

  onToggleItem(event: { ticketId: string; itemId?: string; itemIndex: number }): void {
    const ticket = this.tickets.find((t) => t.id === event.ticketId);
    const item = ticket?.items[event.itemIndex];
    if (!item) return;
    item.prepared = !item.prepared;
    const key = item.id || item.name;
    if (!this.localPrepared.has(event.ticketId)) {
      this.localPrepared.set(event.ticketId, new Set());
    }
    const set = this.localPrepared.get(event.ticketId)!;
    if (item.prepared) {
      set.add(key);
    } else {
      set.delete(key);
    }
    if (event.itemId) {
      this.api
        .patchData(`/api/v1/kitchen/orders/${event.ticketId}/items/${event.itemId}/toggle`, {})
        .subscribe({ error: () => undefined });
    }
  }
}
