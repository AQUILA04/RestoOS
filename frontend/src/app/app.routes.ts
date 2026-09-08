import { Routes } from '@angular/router';
import { PosPageComponent } from './pages/pos/pos-page.component';
import { PosOrderPageComponent } from './pages/pos/pos-order-page.component';
import { KdsPageComponent } from './pages/kds/kds-page.component';
import { AdminCatalogPageComponent } from './pages/admin/admin-catalog-page.component';
import { AdminDashboardPageComponent } from './pages/admin/admin-dashboard-page.component';

export const APP_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'pos' },
  { path: 'pos', component: PosPageComponent },
  { path: 'pos/orders/:orderNumber', component: PosOrderPageComponent },
  { path: 'kds', component: KdsPageComponent },
  { path: 'admin/catalog', component: AdminCatalogPageComponent },
  { path: 'admin/dashboard', component: AdminDashboardPageComponent },
  { path: '**', redirectTo: 'pos' },
];
