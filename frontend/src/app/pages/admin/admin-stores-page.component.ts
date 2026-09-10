import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../core/services/api.service';
import { StoreContextService } from '../../core/services/store-context.service';

type StoreRow = {
  id: string;
  name: string;
  code?: string;
  timezone?: string;
  currency?: string;
  active?: boolean;
};

@Component({
  selector: 'resto-admin-stores-page',
  templateUrl: './admin-stores-page.component.html',
  styleUrls: ['./admin-stores-page.component.css'],
  standalone: false,
})
export class AdminStoresPageComponent implements OnInit {
  stores: StoreRow[] = [];
  newStoreName = '';
  newStoreCurrency = 'EUR';
  newStoreTimezone = 'Europe/Paris';
  edit: Record<string, { name: string; currency: string; timezone: string }> = {};
  message = '';
  error = '';
  activeStoreId = '';

  readonly currencies = ['EUR', 'XOF', 'XAF', 'USD', 'GBP', 'MAD', 'TND', 'GNF', 'CDF'];

  constructor(
    private readonly api: ApiService,
    private readonly storeContext: StoreContextService,
  ) {}

  ngOnInit(): void {
    this.activeStoreId = this.storeContext.storeId || '';
    this.reload();
  }

  reload(): void {
    this.api.getData<StoreRow[]>('/api/v1/stores').subscribe({
      next: (stores) => {
        this.stores = stores || [];
        this.edit = {};
        this.stores.forEach((s) => {
          this.edit[s.id] = {
            name: s.name,
            currency: s.currency || 'EUR',
            timezone: s.timezone || 'UTC',
          };
        });
      },
      error: () => {
        this.error = 'Impossible de charger les établissements.';
      },
    });
  }

  save(store: StoreRow): void {
    const draft = this.edit[store.id];
    const name = (draft?.name || '').trim();
    if (!name) {
      return;
    }
    this.api
      .patchData<StoreRow>(`/api/v1/stores/${store.id}`, {
        name,
        currency: draft.currency,
        timezone: draft.timezone,
      })
      .subscribe({
        next: (updated) => {
          store.name = updated.name;
          store.currency = updated.currency;
          store.timezone = updated.timezone;
          this.message = 'Établissement mis à jour.';
          this.error = '';
        },
        error: () => {
          this.error = 'Mise à jour impossible.';
        },
      });
  }

  /** Alias for existing E2E rename selectors. */
  rename(store: StoreRow): void {
    this.save(store);
  }

  addStore(): void {
    const name = this.newStoreName.trim();
    if (!name) {
      return;
    }
    this.api
      .postData<StoreRow>('/api/v1/stores', {
        name,
        timezone: this.newStoreTimezone || 'Europe/Paris',
        currency: this.newStoreCurrency || 'EUR',
      })
      .subscribe({
        next: () => {
          this.newStoreName = '';
          this.message = 'Établissement ajouté.';
          this.reload();
        },
        error: () => {
          this.error = 'Ajout impossible.';
        },
      });
  }

  selectStore(store: StoreRow): void {
    const session = this.storeContext.session;
    if (!session) {
      return;
    }
    this.storeContext.setSession({ ...session, storeId: store.id });
    this.activeStoreId = store.id;
    this.message = `Restaurant actif : ${store.name}`;
  }
}
