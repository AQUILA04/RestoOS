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
    try {
      const storeId = this.api.requireStoreId();
      this.api.getData<any>('/api/v1/dashboard/metrics', { storeId }).subscribe({
        next: (m) => (this.metrics = m || this.metrics),
        error: () => undefined,
      });
    } catch {
      // Session may be injected after first paint; metrics load on next navigation/reload.
    }
  }
}
