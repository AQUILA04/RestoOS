import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../core/services/api.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'resto-admin-catalog-page',
  templateUrl: './admin-catalog-page.component.html',
  styleUrls: ['./admin-catalog-page.component.css'],
  standalone: false,
})
export class AdminCatalogPageComponent implements OnInit {
  categories: any[] = [];
  products: any[] = [];
  currency = 'EUR';
  toast = '';

  newCategoryName = '';
  productForm = {
    id: '' as string,
    categoryId: '',
    name: '',
    description: '',
    basePrice: '',
    taxRate: '10',
    imageUrl: '',
    avgPrepMinutes: '15',
    active: true,
  };
  editingProductId: string | null = null;
  overridePrice = '';
  showProductForm = false;

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    try {
      const storeId = this.api.requireStoreId();
      const orgId = this.api.requireOrgId();
      forkJoin({
        categories: this.api.getCategories(orgId).pipe(catchError(() => of([]))),
        products: this.api.getProducts(orgId).pipe(catchError(() => of([]))),
        resolved: this.api.getResolvedProducts(storeId, orgId).pipe(catchError(() => of([]))),
        stores: this.api.getData<any[]>('/api/v1/stores').pipe(catchError(() => of([]))),
      }).subscribe({
        next: ({ categories, products, resolved, stores }) => {
          this.categories = categories || [];
          const priceById = new Map(
            (resolved || []).map((p: any) => [p.productId || p.id, p.resolvedPrice ?? p.basePrice]),
          );
          this.products = (products || []).map((p: any) => ({
            ...p,
            resolvedPrice: priceById.get(p.id) ?? p.basePrice,
          }));
          const store = (stores || []).find((s: any) => s.id === storeId);
          this.currency = store?.currency || 'EUR';
          if (!this.productForm.categoryId && this.categories.length) {
            this.productForm.categoryId = this.categories[0].id;
          }
        },
        error: () => {
          this.toast = 'Impossible de charger le catalogue';
        },
      });
    } catch {
      this.toast = 'Contexte magasin manquant';
    }
  }

  categoryName(id: string): string {
    return this.categories.find((c) => c.id === id)?.name || '—';
  }

  addCategory(): void {
    const name = this.newCategoryName.trim();
    if (!name) return;
    this.api.createCategory({ name, displayOrder: this.categories.length }).subscribe({
      next: () => {
        this.newCategoryName = '';
        this.toast = 'Catégorie créée';
        this.reload();
        setTimeout(() => (this.toast = ''), 2500);
      },
      error: (err) => {
        this.toast = err?.error?.message || 'Échec création catégorie';
      },
    });
  }

  deleteCategory(cat: any): void {
    if (!confirm(`Supprimer la catégorie « ${cat.name} » ?`)) return;
    this.api.deleteCategory(cat.id).subscribe({
      next: () => {
        this.toast = 'Catégorie supprimée';
        this.reload();
      },
      error: (err) => {
        this.toast = err?.error?.message || 'Suppression impossible';
      },
    });
  }

  openNewProduct(): void {
    this.showProductForm = true;
    this.productForm = {
      id: '',
      categoryId: this.categories[0]?.id || '',
      name: '',
      description: '',
      basePrice: '',
      taxRate: '10',
      imageUrl: '',
      avgPrepMinutes: '15',
      active: true,
    };
  }

  editProduct(p: any): void {
    this.showProductForm = true;
    this.productForm = {
      id: p.id,
      categoryId: p.categoryId,
      name: p.name || '',
      description: p.description || '',
      basePrice: String(p.basePrice ?? ''),
      taxRate: String(p.taxRate ?? '10'),
      imageUrl: p.imageUrl || '',
      avgPrepMinutes: String(p.avgPrepMinutes ?? 15),
      active: p.active !== false,
    };
  }

  saveProduct(): void {
    const body = {
      categoryId: this.productForm.categoryId,
      name: this.productForm.name.trim(),
      description: this.productForm.description.trim() || null,
      basePrice: Number(this.productForm.basePrice),
      taxRate: Number(this.productForm.taxRate),
      imageUrl: this.productForm.imageUrl.trim() || null,
      avgPrepMinutes: Number(this.productForm.avgPrepMinutes) || 15,
      active: this.productForm.active,
    };
    if (!body.name || !body.categoryId || Number.isNaN(body.basePrice)) {
      this.toast = 'Nom, catégorie et prix sont requis';
      return;
    }
    const req = this.productForm.id
      ? this.api.updateProduct(this.productForm.id, body)
      : this.api.createProduct(body);
    req.subscribe({
      next: () => {
        this.toast = this.productForm.id ? 'Produit mis à jour' : 'Produit créé';
        this.showProductForm = false;
        this.reload();
        setTimeout(() => (this.toast = ''), 2500);
      },
      error: (err) => {
        this.toast = err?.error?.message || 'Échec enregistrement produit';
      },
    });
  }

  deleteProduct(p: any): void {
    if (!confirm(`Supprimer « ${p.name} » ?`)) return;
    this.api.deleteProduct(p.id).subscribe({
      next: () => {
        this.toast = 'Produit supprimé';
        this.reload();
      },
      error: (err) => {
        this.toast = err?.error?.message || 'Suppression impossible';
      },
    });
  }

  startEditOverride(product: any): void {
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
      },
    });
  }
}
