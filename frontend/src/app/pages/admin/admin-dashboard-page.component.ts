import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'resto-admin-dashboard-page',
  templateUrl: './admin-dashboard-page.component.html',
  styleUrls: ['./admin-dashboard-page.component.css'],
  standalone: false,
})
export class AdminDashboardPageComponent implements OnInit {
  metrics: any = {
    todaysOrdersCount: 0,
    declaredRevenue: 0,
    unpaidOrdersCount: 0,
    preparingOrdersCount: 0,
    readyOrdersCount: 0,
    occupiedTablesCount: 0,
  };

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    const storeId = this.api.requireStoreId();
    this.api.getData<any>('/api/v1/dashboard/metrics', { storeId }).subscribe((m) => (this.metrics = m));
  }
}
