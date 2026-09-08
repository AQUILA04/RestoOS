import { Component } from '@angular/core';

@Component({
  selector: 'resto-root',
  template: `
    <header class="shell-header">
      <div class="brand">RestoOS</div>
      <nav>
        <a routerLink="/pos">POS</a>
        <a routerLink="/kds">KDS</a>
        <a routerLink="/admin/catalog">Catalogue</a>
        <a routerLink="/admin/dashboard">Dashboard</a>
      </nav>
    </header>
    <main class="shell-main">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    .shell-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 20px;
      background: #1A1A1A;
      color: white;
    }
    .brand {
      font-family: Outfit, sans-serif;
      font-weight: 700;
      font-size: 1.25rem;
      color: #FF6E40;
    }
    nav { display: flex; gap: 16px; }
    nav a { text-decoration: none; color: #F5F5F5; font-weight: 500; }
    .shell-main { min-height: calc(100vh - 52px); }
  `],
  standalone: false,
})
export class AppComponent {}
