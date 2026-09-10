import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../core/services/api.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'resto-admin-tables-page',
  templateUrl: './admin-tables-page.component.html',
  styleUrls: ['./admin-tables-page.component.css'],
  standalone: false,
})
export class AdminTablesPageComponent implements OnInit {
  zones: any[] = [];
  tables: any[] = [];
  toast = '';
  error = '';

  newZoneName = '';
  newTable = { zoneId: '', tableNumber: '', capacity: '4' };
  editCapacity: Record<string, string> = {};

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    try {
      const storeId = this.api.requireStoreId();
      forkJoin({
        zones: this.api.getZones(storeId).pipe(catchError(() => of([]))),
        tables: this.api.getTables(storeId).pipe(catchError(() => of([]))),
      }).subscribe({
        next: ({ zones, tables }) => {
          this.zones = zones || [];
          this.tables = tables || [];
          this.editCapacity = {};
          this.tables.forEach((t) => (this.editCapacity[t.id] = String(t.capacity ?? 4)));
          if (!this.newTable.zoneId && this.zones.length) {
            this.newTable.zoneId = this.zones[0].id;
          }
        },
        error: () => {
          this.error = 'Impossible de charger le plan de salle.';
        },
      });
    } catch {
      this.error = 'Contexte magasin manquant';
    }
  }

  zoneName(id: string): string {
    return this.zones.find((z) => z.id === id)?.name || '—';
  }

  addZone(): void {
    const name = this.newZoneName.trim();
    if (!name) return;
    const storeId = this.api.requireStoreId();
    this.api.createZone(storeId, { name, displayOrder: this.zones.length }).subscribe({
      next: () => {
        this.newZoneName = '';
        this.toast = 'Zone créée';
        this.reload();
        setTimeout(() => (this.toast = ''), 2500);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Création zone impossible';
      },
    });
  }

  addTable(): void {
    const storeId = this.api.requireStoreId();
    const tableNumber = this.newTable.tableNumber.trim();
    if (!tableNumber || !this.newTable.zoneId) {
      this.error = 'Zone et numéro de table requis';
      return;
    }
    this.api
      .createTable(storeId, {
        zoneId: this.newTable.zoneId,
        tableNumber,
        name: tableNumber,
        capacity: Number(this.newTable.capacity) || 4,
      })
      .subscribe({
        next: () => {
          this.newTable.tableNumber = '';
          this.toast = 'Table créée';
          this.error = '';
          this.reload();
          setTimeout(() => (this.toast = ''), 2500);
        },
        error: (err) => {
          this.error = err?.error?.message || 'Création table impossible';
        },
      });
  }

  saveTable(table: any): void {
    const storeId = this.api.requireStoreId();
    this.api
      .updateTable(storeId, table.id, {
        capacity: Number(this.editCapacity[table.id]) || 4,
        tableNumber: table.tableNumber,
        zoneId: table.zoneId,
        status: table.status,
      })
      .subscribe({
        next: () => {
          this.toast = 'Table mise à jour';
          this.reload();
        },
        error: (err) => {
          this.error = err?.error?.message || 'Mise à jour impossible';
        },
      });
  }

  setStatus(table: any, status: string): void {
    const storeId = this.api.requireStoreId();
    this.api.updateTable(storeId, table.id, { status }).subscribe({
      next: () => {
        table.status = status;
        this.toast = 'Statut mis à jour';
      },
      error: (err) => {
        this.error = err?.error?.message || 'Statut impossible';
      },
    });
  }

  deleteTable(table: any): void {
    if (!confirm(`Supprimer la table ${table.tableNumber} ?`)) return;
    const storeId = this.api.requireStoreId();
    this.api.deleteTable(storeId, table.id).subscribe({
      next: () => {
        this.toast = 'Table supprimée';
        this.reload();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Suppression impossible';
      },
    });
  }
}
