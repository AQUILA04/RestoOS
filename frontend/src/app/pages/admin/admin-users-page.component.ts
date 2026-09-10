import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../core/services/api.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'resto-admin-users-page',
  templateUrl: './admin-users-page.component.html',
  styleUrls: ['./admin-users-page.component.css'],
  standalone: false,
})
export class AdminUsersPageComponent implements OnInit {
  members: any[] = [];
  stores: any[] = [];
  toast = '';
  error = '';

  invite = { email: '', role: 'WAITER', storeId: '' };
  pinDraft: Record<string, string> = {};
  roleDraft: Record<string, string> = {};

  readonly roles = ['OWNER', 'ADMIN', 'STORE_MANAGER', 'CASHIER', 'WAITER', 'KITCHEN'];

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    forkJoin({
      members: this.api.listMembers().pipe(catchError(() => of([]))),
      stores: this.api.getData<any[]>('/api/v1/stores').pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ members, stores }) => {
        this.members = members || [];
        this.stores = stores || [];
        this.roleDraft = {};
        this.members.forEach((m) => (this.roleDraft[m.membershipId] = m.role));
        if (!this.invite.storeId) {
          try {
            this.invite.storeId = this.api.requireStoreId();
          } catch {
            this.invite.storeId = this.stores[0]?.id || '';
          }
        }
      },
      error: () => {
        this.error = 'Impossible de charger les utilisateurs.';
      },
    });
  }

  sendInvite(): void {
    const email = this.invite.email.trim();
    if (!email || !this.invite.role) {
      this.error = 'Email et rôle requis';
      return;
    }
    this.api
      .inviteMember({
        email,
        role: this.invite.role,
        storeId: this.invite.storeId || undefined,
      })
      .subscribe({
        next: () => {
          this.toast = 'Invitation envoyée';
          this.invite.email = '';
          this.error = '';
          this.reload();
          setTimeout(() => (this.toast = ''), 2500);
        },
        error: (err) => {
          this.error = err?.error?.message || 'Invitation impossible';
        },
      });
  }

  saveRole(member: any): void {
    const role = this.roleDraft[member.membershipId];
    this.api
      .updateMembership(member.membershipId, {
        role,
        userId: member.userId,
      })
      .subscribe({
        next: () => {
          member.role = role;
          this.error = '';
          this.toast = 'Rôle mis à jour';
          setTimeout(() => (this.toast = ''), 2500);
        },
        error: (err) => {
          this.error = err?.error?.message || 'Mise à jour rôle impossible';
        },
      });
  }

  toggleActive(member: any): void {
    const active = !(member.active !== false);
    this.api
      .updateMembership(member.membershipId, {
        userId: member.userId,
        active,
      })
      .subscribe({
        next: () => {
          member.active = active;
          this.error = '';
          this.toast = active ? 'Utilisateur réactivé' : 'Utilisateur désactivé';
          setTimeout(() => (this.toast = ''), 2500);
        },
        error: (err) => {
          this.error = err?.error?.message || 'Changement statut impossible';
        },
      });
  }

  savePin(member: any): void {
    const pin = (this.pinDraft[member.userId] || '').trim();
    if (!/^\d{4}$/.test(pin)) {
      this.error = 'Le PIN doit contenir 4 chiffres';
      return;
    }
    this.api.setPin(member.userId, pin).subscribe({
      next: () => {
        member.hasPin = true;
        this.pinDraft[member.userId] = '';
        this.toast = 'PIN enregistré';
        this.error = '';
      },
      error: (err) => {
        this.error = err?.error?.message || 'PIN impossible';
      },
    });
  }
}
