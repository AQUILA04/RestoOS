import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../core/services/api.service';

type PaymentMethod = 'CARD' | 'MOBILE_MONEY' | 'CASH';

@Component({
  selector: 'resto-pos-order-page',
  templateUrl: './pos-order-page.component.html',
  styleUrls: ['./pos-order-page.component.css'],
  standalone: false,
})
export class PosOrderPageComponent implements OnInit {
  order: any = null;
  orderStatus = '';
  clientEmail = '';
  errorMessage = '';
  tables: Array<{ id: string; name: string }> = [];
  selectedTableId = '';
  mobileMoneyLabel = 'Transfert Mobile';
  paymentPanelOpen = false;
  cashAmountReceived: number | null = null;
  lastChange: number | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly api: ApiService,
  ) {}

  ngOnInit(): void {
    const orderNumber = this.route.snapshot.paramMap.get('orderNumber');
    const storeId = this.api.requireStoreId();
    const orgId = this.api.requireOrgId();

    this.api.getData<any[]>('/api/v1/orders', { storeId }).subscribe((orders) => {
      this.order = orders.find((o) => String(o.orderNumber) === String(orderNumber)?.replace('#', ''));
      if (this.order) {
        this.orderStatus = this.mapStatus(this.order.status, this.order.paymentStatus);
        this.selectedTableId = this.order.tableId || '';
      }
    });

    this.api.getTables(storeId).subscribe({
      next: (tables) => {
        this.tables = (tables || []).map((t) => ({
          id: t.id,
          name: t.name || t.tableNumber || t.id,
        }));
      },
      error: () => (this.tables = []),
    });

    this.api.getOrganization(orgId).subscribe({
      next: (org) => {
        if (org?.mobileMoneyLabel) {
          this.mobileMoneyLabel = org.mobileMoneyLabel;
        }
      },
      error: () => undefined,
    });
  }

  get canDeliver(): boolean {
    return !!this.order && this.order.status === 'READY' && this.order.paymentStatus !== 'PAID';
  }

  get canPay(): boolean {
    return !!this.order && this.order.paymentStatus !== 'PAID' && this.order.status !== 'CANCELLED';
  }

  get canAssignTable(): boolean {
    return !!this.order
      && this.order.orderType === 'DINE_IN'
      && this.order.paymentStatus !== 'PAID'
      && this.order.status !== 'CANCELLED'
      && this.order.status !== 'CLOSED';
  }

  get cashChange(): number | null {
    if (this.cashAmountReceived == null || !this.order) return null;
    const change = Number(this.cashAmountReceived) - Number(this.order.totalAmount);
    return Math.round(change * 100) / 100;
  }

  get cashEnough(): boolean {
    return this.cashChange != null && this.cashChange >= 0;
  }

  assignTable(): void {
    if (!this.order) return;
    const tableId = this.selectedTableId || null;
    this.api.updateOrderTable(this.order.id, tableId).subscribe({
      next: (order) => {
        this.order = { ...this.order, ...order };
        this.errorMessage = '';
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Impossible d’assigner la table';
      },
    });
  }

  deliver(): void {
    if (!this.order || !this.canDeliver) return;
    this.api.deliverOrder(this.order.id).subscribe({
      next: (order) => {
        this.order = { ...this.order, ...order, status: order?.status || 'DELIVERED' };
        this.orderStatus = 'LIVRÉ';
        this.errorMessage = '';
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Livraison impossible';
      },
    });
  }

  openPayment(): void {
    this.paymentPanelOpen = true;
    this.cashAmountReceived = null;
    this.lastChange = null;
    this.errorMessage = '';
  }

  payWith(method: PaymentMethod): void {
    if (!this.order) return;
    if (method === 'CASH') {
      if (!this.cashEnough || this.cashAmountReceived == null) {
        this.errorMessage = 'Montant reçu insuffisant';
        return;
      }
      this.confirmPayment('CASH', Number(this.cashAmountReceived));
      return;
    }
    this.confirmPayment(method);
  }

  private confirmPayment(method: PaymentMethod, amountTendered?: number): void {
    const key = crypto.randomUUID();
    const body: Record<string, unknown> = {
      paymentMethod: method,
      amount: this.order.totalAmount,
      clientEmail: this.clientEmail,
    };
    if (method === 'CASH' && amountTendered != null) {
      body['amountTendered'] = amountTendered;
    }
    this.api.markPaid(this.order.id, body, key).subscribe({
      next: (payment) => {
        this.order = {
          ...this.order,
          ...(payment.order || {}),
          paymentStatus: 'PAID',
          status: 'CLOSED',
        };
        this.orderStatus = 'PAYÉ';
        this.paymentPanelOpen = false;
        this.lastChange = method === 'CASH' ? this.cashChange : null;
        this.errorMessage = '';
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Paiement impossible';
      },
    });
  }

  private mapStatus(status: string, paymentStatus: string): string {
    if (paymentStatus === 'PAID') return 'PAYÉ';
    if (status === 'DELIVERED') return 'LIVRÉ';
    if (status === 'READY') return 'PRÊT À LIVRER';
    if (status === 'SENT_TO_KITCHEN' || status === 'PREPARING') return 'EN CUISINE';
    if (status === 'CLOSED') return 'CLÔTURÉE';
    return status;
  }
}
