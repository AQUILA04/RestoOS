import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { ApiService } from '../../core/services/api.service';
import { StoreContextService } from '../../core/services/store-context.service';
import { OfflineQueueService } from '../../core/services/offline-queue.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { CartItem } from '../../ui-components/pos-layout/pos-layout.component';
import { TableNode, ZoneTab } from '../../ui-components/floor-plan/floor-plan.component';
import { ModifierGroupItem, ModifierOptionItem } from '../../ui-components/modifier-modal/modifier-modal.component';

@Component({
  selector: 'resto-pos-page',
  templateUrl: './pos-page.component.html',
  styleUrls: ['./pos-page.component.css'],
  standalone: false,
})
export class PosPageComponent implements OnInit, OnDestroy {
  authenticated = false;
  activeTab: 'floor' | 'catalog' | 'orders' = 'floor';
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
  lastCreatedOrderNumber: number | null = null;
  orderStatus = '';
  errorMessage = '';
  isOnline = true;
  pendingCount = 0;
  openOrders: Array<{
    id: string;
    orderNumber: number;
    tableId?: string;
    totalAmount?: number;
    status: string;
    paymentStatus: string;
  }> = [];

  cashSession: any | null = null;
  cashSessionChecked = false;
  openCashModal = false;
  closeCashModal = false;
  reportModal = false;
  openingFloatInput: number | null = null;
  closingNotes = '';
  cashBusy = false;
  sessionReport: any | null = null;
  shareHint = '';

  get revenueByMethodEntries(): Array<{ key: string; value: number }> {
    const byMethod = this.sessionReport?.revenueByMethod;
    if (!byMethod || typeof byMethod !== 'object') {
      return [];
    }
    return Object.keys(byMethod).map((key) => ({
      key,
      value: Number(byMethod[key] ?? 0),
    }));
  }

  private wsSub?: Subscription;

  constructor(
    private readonly auth: AuthService,
    private readonly api: ApiService,
    private readonly storeContext: StoreContextService,
    private readonly offlineQueue: OfflineQueueService,
    private readonly ws: WebSocketService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.authenticated = !!this.storeContext.session || localStorage.getItem('resto_authenticated') === 'true';
    this.offlineQueue.isOnline().subscribe((v) => (this.isOnline = v));
    this.offlineQueue.getQueue().subscribe((q) => (this.pendingCount = q.length));
    if (this.authenticated) {
      this.loadOperationalData();
      this.checkCashSession();
    } else {
      this.loadStaffForPin();
    }
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
    this.ws.disconnect();
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
        this.checkCashSession();
      },
      error: () => {
        this.errorMessage = 'PIN invalide';
      },
    });
  }

  checkCashSession(): void {
    let storeId: string;
    try {
      storeId = this.api.requireStoreId();
    } catch {
      return;
    }
    this.cashSessionChecked = false;
    this.api.getCurrentCashSession(storeId).subscribe({
      next: (session) => {
        this.cashSession = session;
        this.cashSessionChecked = true;
        this.openCashModal = !session;
      },
      error: () => {
        this.cashSession = null;
        this.cashSessionChecked = true;
        this.openCashModal = true;
      },
    });
  }

  confirmOpenCashSession(): void {
    let storeId: string;
    try {
      storeId = this.api.requireStoreId();
    } catch {
      this.errorMessage = 'Contexte magasin manquant';
      return;
    }
    this.cashBusy = true;
    this.errorMessage = '';
    this.api.openCashSession(storeId, this.openingFloatInput).subscribe({
      next: (session) => {
        this.cashSession = session;
        this.openCashModal = false;
        this.cashBusy = false;
        this.openingFloatInput = null;
      },
      error: (err) => {
        this.cashBusy = false;
        this.errorMessage = err?.error?.message || "Impossible d'ouvrir la caisse";
      },
    });
  }

  askCloseCashSession(): void {
    if (!this.cashSession?.id) {
      return;
    }
    this.closingNotes = '';
    this.closeCashModal = true;
  }

  confirmCloseCashSession(): void {
    if (!this.cashSession?.id) {
      return;
    }
    this.cashBusy = true;
    this.errorMessage = '';
    this.api.closeCashSession(this.cashSession.id, this.closingNotes).subscribe({
      next: (session) => {
        this.cashBusy = false;
        this.closeCashModal = false;
        this.cashSession = null;
        this.sessionReport = session?.report || null;
        this.reportModal = true;
        this.shareHint = '';
      },
      error: (err) => {
        this.cashBusy = false;
        this.errorMessage = err?.error?.message || 'Impossible de fermer la caisse';
      },
    });
  }

  downloadSessionPdf(): void {
    const sessionId = this.sessionReport?.sessionId || this.cashSession?.id;
    if (!sessionId) {
      return;
    }
    this.api.downloadCashSessionReportPdf(sessionId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `rapport-caisse-${sessionId}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.errorMessage = 'Téléchargement PDF impossible';
      },
    });
  }

  async shareSessionReport(): Promise<void> {
    const sessionId = this.sessionReport?.sessionId || this.cashSession?.id;
    if (!sessionId) {
      return;
    }
    this.shareHint = '';
    try {
      const blob = await this.api.downloadCashSessionReportPdf(sessionId).toPromise();
      if (!blob) {
        throw new Error('empty pdf');
      }
      const file = new File([blob], `rapport-caisse-${sessionId}.pdf`, { type: 'application/pdf' });
      const nav: any = navigator;
      if (nav.share && (!nav.canShare || nav.canShare({ files: [file] }))) {
        await nav.share({
          title: 'Rapport de caisse',
          text: `Rapport de caisse — CA ${this.sessionReport?.totalRevenue ?? ''}`,
          files: [file],
        });
        return;
      }
      this.downloadSessionPdf();
      this.shareHint = 'Partage natif indisponible — PDF téléchargé.';
    } catch {
      this.downloadSessionPdf();
      this.shareHint = 'Partage impossible — PDF téléchargé.';
    }
  }

  dismissReportModal(): void {
    this.reportModal = false;
    this.sessionReport = null;
    this.openCashModal = true;
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

    this.reloadOpenOrders(storeId);
    this.ws.connect(storeId, 'pos');
    this.wsSub?.unsubscribe();
    this.wsSub = this.ws.getMessages().subscribe(() => {
      this.reloadOpenOrders(storeId);
    });
  }

  get readyCount(): number {
    return this.openOrders.filter((o) => o.status === 'READY').length;
  }

  get unpaidCount(): number {
    return this.openOrders.filter((o) => o.paymentStatus !== 'PAID').length;
  }

  reloadOpenOrders(storeId?: string): void {
    const sid = storeId || this.storeContext.storeId;
    if (!sid) return;
    this.api.getData<any[]>('/api/v1/orders', { storeId: sid }).subscribe({
      next: (orders) => {
        this.openOrders = (orders || [])
          .filter((o) => o.status !== 'CLOSED' && o.status !== 'CANCELLED')
          .map((o) => ({
            id: o.id,
            orderNumber: o.orderNumber,
            tableId: o.tableId,
            totalAmount: o.totalAmount,
            status: o.status,
            paymentStatus: o.paymentStatus || 'UNPAID',
          }))
          .sort((a, b) => {
            const rank = (s: string) => (s === 'READY' ? 0 : s === 'DELIVERED' ? 1 : 2);
            const byStatus = rank(a.status) - rank(b.status);
            return byStatus !== 0 ? byStatus : b.orderNumber - a.orderNumber;
          });
      },
      error: () => (this.openOrders = []),
    });
  }

  /** @deprecated use reloadOpenOrders — kept for template compatibility during rename */
  reloadReadyOrders(storeId?: string): void {
    this.reloadOpenOrders(storeId);
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

  clearTableSelection(): void {
    this.selectedTable = null;
  }

  startOrderWithoutTable(): void {
    this.selectedTable = null;
    this.activeTab = 'catalog';
  }

  openOrder(order: { orderNumber: number }): void {
    void this.router.navigate(['/pos/orders', order.orderNumber]);
  }

  openLastCreatedOrder(): void {
    if (this.lastCreatedOrderNumber != null) {
      void this.router.navigate(['/pos/orders', this.lastCreatedOrderNumber]);
    }
  }

  statusLabel(status: string, paymentStatus: string): string {
    if (paymentStatus === 'PAID' && status !== 'CLOSED') return `${this.opsLabel(status)} · PAYÉ`;
    return this.opsLabel(status);
  }

  private opsLabel(status: string): string {
    if (status === 'READY') return 'PRÊT';
    if (status === 'DELIVERED') return 'LIVRÉ';
    if (status === 'PREPARING' || status === 'SENT_TO_KITCHEN' || status === 'CREATED') return 'EN CUISINE';
    return status;
  }

  onProductSelect(product: any): void {
    this.pendingProduct = product;
    this.modifierProductName = product.name;
    this.modifierGroups = (product.modifierGroups || []).map((g: any) => ({
      id: g.id,
      name: g.name,
      required: !!g.required,
      minSelection: g.minSelection ?? 0,
      maxSelection: g.maxSelection ?? 1,
      options: (g.options || []).map((o: any) => ({
        id: o.id,
        name: o.name,
        priceDelta: Number(o.priceDelta || 0),
      })),
    }));
    if (this.modifierGroups.length) {
      this.modifierOpen = true;
    } else {
      this.addToCart(product, []);
    }
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

  tableLabel(tableId?: string): string {
    if (!tableId) return 'Sans table';
    const t = this.tables.find((x) => x.id === tableId);
    return t ? `Table ${t.name || t.tableNumber}` : 'Table';
  }

  async submitOrder(): Promise<void> {
    if (!this.cartItems.length) {
      this.errorMessage = 'Ajoutez au moins un article';
      return;
    }
    const payload: Record<string, unknown> = {
      organizationId: this.storeContext.organizationId,
      storeId: this.storeContext.storeId,
      orderType: 'DINE_IN',
      sendToKitchen: true,
      items: this.cartItems.map((item: any) => ({
        productId: item.productId,
        quantity: item.quantity,
        modifierOptionIds: (item.modifierOptionIds || []).filter(Boolean),
      })),
    };
    if (this.selectedTable?.id) {
      payload['tableId'] = this.selectedTable.id;
    }
    const idempotencyKey = crypto.randomUUID();
    if (!navigator.onLine) {
      await this.offlineQueue.enqueueOrder({ ...payload, idempotencyKey });
      this.orderStatus = 'HORS LIGNE';
      return;
    }
    this.api.createOrder(payload, idempotencyKey).subscribe({
      next: (order) => {
        this.orderNumberDisplay = `#${order.orderNumber}`;
        this.lastCreatedOrderNumber = order.orderNumber;
        this.orderStatus = 'EN CUISINE';
        this.cartItems = [];
        this.errorMessage = '';
        this.reloadOpenOrders();
      },
      error: async (err) => {
        await this.offlineQueue.enqueueOrder({ ...payload, idempotencyKey });
        this.errorMessage = err?.error?.message || 'Commande mise en file hors-ligne';
      },
    });
  }
}
