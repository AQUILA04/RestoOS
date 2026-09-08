import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'resto-admin-catalog-page',
  templateUrl: './admin-catalog-page.component.html',
  styleUrls: ['./admin-catalog-page.component.css'],
  standalone: false,
})
export class AdminCatalogPageComponent implements OnInit {
  products: any[] = [];
  editingProductId: string | null = null;
  overridePrice = '';
  toast = '';

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    const storeId = this.api.requireStoreId();
    this.api.getResolvedProducts(storeId, this.api.requireOrgId()).subscribe((products) => {
      this.products = (products || []).map((p) => ({
        ...p,
        id: p.productId || p.id,
      }));
    });
  }

  startEdit(product: any): void {
    this.editingProductId = product.id;
    this.overridePrice = String(product.resolvedPrice ?? product.basePrice ?? '');
  }

  saveOverride(product: any): void {
    const storeId = this.api.requireStoreId();
    this.api.setStorePriceOverride(storeId, product.id, Number(this.overridePrice)).subscribe({
      next: () => {
        this.toast = 'Prix local mis à jour';
        this.editingProductId = null;
        this.reload();
        setTimeout(() => (this.toast = ''), 2500);
      },
      error: (err) => {
        this.toast = err?.error?.message || 'Échec de la mise à jour du prix';
        setTimeout(() => (this.toast = ''), 2500);
      },
    });
  }
}
