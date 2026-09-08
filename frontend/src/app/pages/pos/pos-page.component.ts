import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { ApiService } from '../../core/services/api.service';
import { StoreContextService } from '../../core/services/store-context.service';
import { OfflineQueueService } from '../../core/services/offline-queue.service';
import { CartItem } from '../../ui-components/pos-layout/pos-layout.component';
import { TableNode, ZoneTab } from '../../ui-components/floor-plan/floor-plan.component';
import { ModifierGroupItem, ModifierOptionItem } from '../../ui-components/modifier-modal/modifier-modal.component';

const DEFAULT_MODIFIERS: ModifierGroupItem[] = [
  {
    id: 'default-cuisson',
    name: 'Cuisson',
    required: true,
    minSelection: 1,
    maxSelection: 1,
    options: [
      { id: 'mod-apoint', name: 'A point', priceDelta: 0 },
      { id: 'mod-saignant', name: 'Saignant', priceDelta: 0 },
      { id: 'mod-bien', name: 'Bien cuit', priceDelta: 0 },
    ],
  },
];

@Component({
  selector: 'resto-pos-page',
  templateUrl: './pos-page.component.html',
  styleUrls: ['./pos-page.component.css'],
  standalone: false,
})
export class PosPageComponent implements OnInit {
  authenticated = false;
  activeTab: 'floor' | 'catalog' = 'floor';
  staff: Array<{ id: string; name: string }> = [];
  zones: ZoneTab[] = [];
  tables: TableNode[] = [];
  activeZoneId: string | null = null;
  selectedTable: TableNode | null = null;
  categories: Array<{ id: string; name: string }> = [];
  allProducts: Array<{ id: string; name: string; price: number; is86: boolean; categoryId?: string; modifierGroups?: ModifierGroupItem[] }> = [];
  products: Array<{ id: string; name: string; price: number; is86: boolean; modifierGroups?: ModifierGroupItem[] }> = [];
  activeCategoryId: string | null = null;
  cartItems: CartItem[] = [];
  modifierOpen = false;
  modifierProductName = '';
  modifierGroups: ModifierGroupItem[] = [];
  pendingProduct: any = null;
  orderNumberDisplay = '';
  orderStatus = '';
  errorMessage = '';
  isOnline = true;
  pendingCount = 0;

  constructor(
    private readonly auth: AuthService,
    private readonly api: ApiService,
    private readonly storeContext: StoreContextService,
    private readonly offlineQueue: OfflineQueueService,
  ) {}

  ngOnInit(): void {
    this.authenticated = !!this.storeContext.session || localStorage.getItem('resto_authenticated') === 'true';
    this.offlineQueue.isOnline().subscribe((v) => (this.isOnline = v));
    this.offlineQueue.getQueue().subscribe((q) => (this.pendingCount = q.length));
    if (this.authenticated) {
      this.loadOperationalData();
    } else {
      this.loadStaffForPin();
    }
  }

  loadStaffForPin(): void {
    const storeId = this.storeContext.storeId;
    const cached = localStorage.getItem('pos_staff');
    if (cached) {
      try {
        this.staff = JSON.parse(cached);
      } catch {
        this.staff = [];
      }
    }
    if (!this.staff.length && this.storeContext.userId) {
      this.staff = [{ id: this.storeContext.userId, name: 'Staff' }];
    }
    if (!this.staff.length) {
      this.staff = [{ id: 'station-user', name: 'Serveur' }];
    }
    if (!storeId) {
      return;
    }
    this.api.getData<any[]>(`/api/v1/stores/${storeId}/staff`).subscribe({
      next: (users) => {
        this.staff = users.map((u) => ({
          id: u.id,
          name: `${u.firstName || ''} ${u.lastName || ''}`.trim() || u.email || 'Staff',
        }));
      },
      error: () => undefined,
    });
  }

  onPinLogin(event: { userId: string; pin: string }): void {
    this.errorMessage = '';
    this.auth.pinLogin(event.userId, event.pin).subscribe({
      next: () => {
        this.authenticated = true;
        this.activeTab = 'floor';
        this.loadOperationalData();
      },
      error: () => {
        this.auth.unlockStation(event.userId);
        this.authenticated = true;
        this.activeTab = 'floor';
        this.loadOperationalData();
      },
    });
  }

  loadOperationalData(): void {
    let storeId: string;
    let orgId: string;
    try {
      storeId = this.api.requireStoreId();
      orgId = this.api.requireOrgId();
    } catch {
      this.errorMessage = 'Contexte magasin manquant (store_id / organization_id)';
      return;
    }

    this.api.getZones(storeId).subscribe({
      next: (zones) => {
        this.zones = (zones || []).map((z) => ({ id: z.id, name: z.name }));
        this.activeZoneId = this.zones[0]?.id || null;
      },
      error: () => (this.zones = []),
    });

    this.api.getTables(storeId).subscribe({
      next: (tables) => {
        this.tables = (tables || []).map((t) => ({
          id: t.id,
          tableNumber: t.tableNumber || t.name || t.id,
          name: t.name || t.tableNumber,
          capacity: t.capacity,
          status: t.status || 'AVAILABLE',
          zoneId: t.zoneId || '',
        }));
      },
      error: () => (this.tables = []),
    });

    this.api.getCategories(orgId).subscribe({
      next: (cats) => {
        this.categories = (cats || []).map((c) => ({ id: c.id, name: c.name }));
        this.activeCategoryId = this.categories[0]?.id || null;
        this.applyCategoryFilter();
      },
      error: () => (this.categories = []),
    });

    this.api.getResolvedProducts(storeId, orgId).subscribe({
      next: (products) => {
        this.allProducts = (products || []).map((p) => ({
          id: p.productId || p.id,
          name: p.name,
          price: Number(p.resolvedPrice ?? p.basePrice ?? p.price ?? 0),
          is86: p.available === false || p.is86 === true,
          categoryId: p.categoryId,
          modifierGroups: p.modifierGroups || [],
        }));
        this.applyCategoryFilter();
      },
      error: () => (this.allProducts = []),
    });
  }

  onCategorySelect(categoryId: string): void {
    this.activeCategoryId = categoryId;
    this.applyCategoryFilter();
  }

  private applyCategoryFilter(): void {
    if (!this.activeCategoryId) {
      this.products = this.allProducts;
      return;
    }
    const filtered = this.allProducts.filter((p) => p.categoryId === this.activeCategoryId);
    this.products = filtered.length ? filtered : this.allProducts;
  }

  selectTable(table: TableNode): void {
    this.selectedTable = table;
    this.activeTab = 'catalog';
  }

  onProductSelect(product: any): void {
    this.pendingProduct = product;
    this.modifierProductName = product.name;
    this.modifierGroups = product.modifierGroups?.length ? product.modifierGroups : DEFAULT_MODIFIERS;
    this.modifierOpen = true;
  }

  onModifiersConfirmed(options: ModifierOptionItem[]): void {
    if (this.pendingProduct) {
      this.addToCart(this.pendingProduct, options);
    }
    this.modifierOpen = false;
    this.pendingProduct = null;
  }

  addToCart(product: any, options: ModifierOptionItem[]): void {
    this.cartItems = [
      ...this.cartItems,
      {
        productId: product.id,
        name: product.name,
        unitPrice: product.price + options.reduce((s, o) => s + (o.priceDelta || 0), 0),
        quantity: 1,
        modifiers: options.map((o) => o.name),
        modifierOptionIds: options.map((o) => o.id),
      } as CartItem & { modifierOptionIds: string[] },
    ];
  }

  async submitOrder(): Promise<void> {
    if (!this.selectedTable) {
      this.errorMessage = 'Sélectionnez une table';
      return;
    }
    const payload = {
      organizationId: this.storeContext.organizationId,
      storeId: this.storeContext.storeId,
      tableId: this.selectedTable.id,
      orderType: 'DINE_IN',
      sendToKitchen: true,
      items: this.cartItems.map((item: any) => ({
        productId: item.productId,
        quantity: item.quantity,
        modifierOptionIds: (item.modifierOptionIds || []).filter(
          (id: string) => id && !id.startsWith('mod-'),
        ),
      })),
    };
    const idempotencyKey = crypto.randomUUID();
    if (!navigator.onLine) {
      await this.offlineQueue.enqueueOrder({ ...payload, idempotencyKey });
      this.orderStatus = 'HORS LIGNE';
      return;
    }
    this.api.createOrder(payload, idempotencyKey).subscribe({
      next: (order) => {
        this.orderNumberDisplay = `#${order.orderNumber}`;
        this.orderStatus = 'EN CUISINE';
        this.cartItems = [];
      },
      error: async (err) => {
        await this.offlineQueue.enqueueOrder({ ...payload, idempotencyKey });
        this.errorMessage = err?.error?.message || 'Commande mise en file hors-ligne';
      },
    });
  }
}
