import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';

export interface KdsTicketItem {
  id?: string;
  name: string;
  quantity: number;
  modifiers?: string[];
  prepared?: boolean;
}

export interface KdsTicket {
  id: string;
  orderNumber: number;
  orderType: string;
  status: 'SENT_TO_KITCHEN' | 'PREPARING' | 'READY';
  createdAt: string;
  items: KdsTicketItem[];
}

@Component({
  selector: 'resto-kds-grid',
  templateUrl: './kds-grid.component.html',
  styleUrls: ['./kds-grid.component.css'],
  standalone: false
})
export class KdsGridComponent implements OnInit, OnDestroy {
  @Input() tickets: KdsTicket[] = [];
  @Output() onAdvanceTicket = new EventEmitter<{ ticketId: string; nextStatus: string }>();
  @Output() onToggleItem = new EventEmitter<{ ticketId: string; itemId?: string; itemIndex: number }>();

  private timerInterval: ReturnType<typeof setInterval> | null = null;
  now: number = Date.now();

  ngOnInit(): void {
    this.timerInterval = setInterval(() => {
      this.now = Date.now();
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  getElapsedMinutes(createdAtIso: string): number {
    const created = new Date(createdAtIso).getTime();
    if (Number.isNaN(created)) return 0;
    return Math.max(0, Math.floor((this.now - created) / 60000));
  }

  getTimerTier(createdAtIso: string): 'tier-green' | 'tier-amber' | 'tier-crimson' {
    const minutes = this.getElapsedMinutes(createdAtIso);
    if (minutes < 8) return 'tier-green';
    if (minutes < 15) return 'tier-amber';
    return 'tier-crimson';
  }

  advance(ticket: KdsTicket): void {
    this.onAdvanceTicket.emit({ ticketId: ticket.id, nextStatus: 'READY' });
  }

  toggleItem(ticket: KdsTicket, itemIndex: number): void {
    const item = ticket.items[itemIndex];
    this.onToggleItem.emit({ ticketId: ticket.id, itemId: item?.id, itemIndex });
  }
}
