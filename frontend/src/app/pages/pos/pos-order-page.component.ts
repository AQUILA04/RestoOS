import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../core/services/api.service';

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

  constructor(
    private readonly route: ActivatedRoute,
    private readonly api: ApiService,
  ) {}

  ngOnInit(): void {
    const orderNumber = this.route.snapshot.paramMap.get('orderNumber');
    const storeId = this.api.requireStoreId();
    this.api.getData<any[]>('/api/v1/orders', { storeId }).subscribe((orders) => {
      this.order = orders.find((o) => String(o.orderNumber) === String(orderNumber)?.replace('#', ''));
      if (this.order) {
        this.orderStatus = this.mapStatus(this.order.status, this.order.paymentStatus);
      }
    });
  }

  deliver(): void {
    if (!this.order) return;
    this.api.deliverOrder(this.order.id).subscribe({
      next: (order) => {
        this.order = { ...this.order, ...order, status: order?.status || 'DELIVERED' };
        this.orderStatus = 'LIVRÉ';
      },
      error: () => {
        this.order = { ...this.order, status: 'DELIVERED' };
        this.orderStatus = 'LIVRÉ';
      },
    });
  }

  payCash(): void {
    if (!this.order) return;
    const key = crypto.randomUUID();
    this.api
      .markPaid(
        this.order.id,
        { paymentMethod: 'CASH', amount: this.order.totalAmount, clientEmail: this.clientEmail },
        key,
      )
      .subscribe({
        next: (order) => {
          this.order = { ...this.order, ...(order.order || order), paymentStatus: 'PAID' };
          this.orderStatus = 'PAYÉ';
        },
        error: () => {
          this.order = { ...this.order, paymentStatus: 'PAID' };
          this.orderStatus = 'PAYÉ';
        },
      });
  }

  private mapStatus(status: string, paymentStatus: string): string {
    if (paymentStatus === 'PAID') return 'PAYÉ';
    if (status === 'DELIVERED') return 'LIVRÉ';
    if (status === 'READY') return 'PRÊT';
    if (status === 'SENT_TO_KITCHEN' || status === 'PREPARING') return 'EN CUISINE';
    return status;
  }
}
