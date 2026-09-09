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
  mobileMoneyLabel = '';
  settingsToast = '';

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    try {
      const storeId = this.api.requireStoreId();
      const orgId = this.api.requireOrgId();
      this.api.getData<any>('/api/v1/dashboard/metrics', { storeId }).subscribe({
        next: (m) => (this.metrics = m || this.metrics),
        error: () => undefined,
      });
      this.api.getOrganization(orgId).subscribe({
        next: (org) => {
          this.mobileMoneyLabel = org?.mobileMoneyLabel || '';
        },
        error: () => undefined,
      });
    } catch {
      // Session may be injected after first paint; metrics load on next navigation/reload.
    }
  }

  saveMobileMoneyLabel(): void {
    try {
      const orgId = this.api.requireOrgId();
      this.api.updateOrganization(orgId, { mobileMoneyLabel: this.mobileMoneyLabel }).subscribe({
        next: (org) => {
          this.mobileMoneyLabel = org?.mobileMoneyLabel || this.mobileMoneyLabel;
          this.settingsToast = 'Libellé enregistré';
          setTimeout(() => (this.settingsToast = ''), 2500);
        },
        error: (err) => {
          this.settingsToast = err?.error?.message || 'Échec de la sauvegarde';
          setTimeout(() => (this.settingsToast = ''), 2500);
        },
      });
    } catch {
      this.settingsToast = 'Contexte organisation manquant';
    }
  }
}
