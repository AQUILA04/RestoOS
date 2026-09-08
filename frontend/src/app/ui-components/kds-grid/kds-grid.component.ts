import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';

export interface KdsTicketItem {
  name: string;
  quantity: number;
  modifiers?: string[];
}

export interface KdsTicket {
  id: string;
  orderNumber: number;
  orderType: string;
  status: 'SENT_TO_KITCHEN' | 'PREPARING' | 'READY';
  createdAt: string; // ISO date string
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

  private timerInterval: any;
  now: number = Date.now();

  ngOnInit(): void {
    this.timerInterval = setInterval(() => {
      this.now = Date.now();
    }, 10000);
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  getElapsedMinutes(createdAtIso: string): number {
    const created = new Date(createdAtIso).getTime();
    return Math.floor((this.now - created) / 60000);
  }

  getTimerTier(createdAtIso: string): 'tier-green' | 'tier-amber' | 'tier-crimson' {
    const minutes = this.getElapsedMinutes(createdAtIso);
    if (minutes < 8) return 'tier-green';
    if (minutes < 15) return 'tier-amber';
    return 'tier-crimson';
  }

  advance(ticket: KdsTicket): void {
    let nextStatus = 'PREPARING';
    if (ticket.status === 'PREPARING') {
      nextStatus = 'READY';
    }
    this.onAdvanceTicket.emit({ ticketId: ticket.id, nextStatus });
  }
}
