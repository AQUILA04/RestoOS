import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../core/services/api.service';
import { StoreContextService } from '../../core/services/store-context.service';

type StoreRow = { id: string; name: string; code?: string; active?: boolean };

@Component({
  selector: 'resto-admin-stores-page',
  templateUrl: './admin-stores-page.component.html',
  styleUrls: ['./admin-stores-page.component.css'],
  standalone: false,
})
export class AdminStoresPageComponent implements OnInit {
  stores: StoreRow[] = [];
  newStoreName = '';
  editName: Record<string, string> = {};
  message = '';
  error = '';
  activeStoreId = '';

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
        this.editName = {};
        this.stores.forEach((s) => (this.editName[s.id] = s.name));
      },
      error: () => {
        this.error = 'Impossible de charger les établissements.';
      },
    });
  }

  rename(store: StoreRow): void {
    const name = (this.editName[store.id] || '').trim();
    if (!name) {
      return;
    }
    this.api.patchData<StoreRow>(`/api/v1/stores/${store.id}`, { name }).subscribe({
      next: (updated) => {
        store.name = updated.name;
        this.message = 'Nom mis à jour.';
        this.error = '';
      },
      error: () => {
        this.error = 'Renommage impossible.';
      },
    });
  }

  addStore(): void {
    const name = this.newStoreName.trim();
    if (!name) {
      return;
    }
    this.api
      .postData<StoreRow>('/api/v1/stores', {
        name,
        timezone: 'Europe/Paris',
        currency: 'EUR',
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
