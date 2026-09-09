import { Component } from '@angular/core';

@Component({
  selector: 'resto-root',
  template: `
    <header class="shell-header">
      <div class="brand">RestoOS</div>
      <button
        type="button"
        class="nav-toggle"
        [attr.aria-expanded]="navOpen"
        aria-controls="shell-nav"
        aria-label="Menu de navigation"
        (click)="navOpen = !navOpen">
        <span class="nav-toggle-bar"></span>
        <span class="nav-toggle-bar"></span>
        <span class="nav-toggle-bar"></span>
      </button>
      <nav id="shell-nav" class="shell-nav" [class.open]="navOpen">
        <a routerLink="/pos" (click)="closeNav()">POS</a>
        <a routerLink="/kds" (click)="closeNav()">KDS</a>
        <a routerLink="/admin/catalog" (click)="closeNav()">Catalogue</a>
        <a routerLink="/admin/dashboard" (click)="closeNav()">Dashboard</a>
      </nav>
    </header>
    <main class="shell-main">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
      max-width: 100%;
      min-height: 100dvh;
      overflow-x: clip;
    }

    .shell-header {
      position: sticky;
      top: 0;
      z-index: 200;
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      align-items: center;
      gap: 12px;
      min-height: var(--shell-header-height, 52px);
      padding: 10px 16px;
      background: #1A1A1A;
      color: white;
    }

    .brand {
      font-family: Outfit, sans-serif;
      font-weight: 700;
      font-size: 1.25rem;
      color: #FF6E40;
      letter-spacing: -0.02em;
    }

    .nav-toggle {
      display: none;
      flex-direction: column;
      justify-content: center;
      gap: 5px;
      width: 44px;
      height: 44px;
      margin: 0;
      padding: 10px;
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 8px;
      background: transparent;
      color: #F5F5F5;
    }

    .nav-toggle-bar {
      display: block;
      width: 100%;
      height: 2px;
      background: currentColor;
      border-radius: 1px;
    }

    .shell-nav {
      display: flex;
      flex-wrap: wrap;
      gap: 8px 16px;
      align-items: center;
    }

    .shell-nav a {
      text-decoration: none;
      color: #F5F5F5;
      font-weight: 500;
      padding: 8px 4px;
      min-height: 44px;
      display: inline-flex;
      align-items: center;
    }

    .shell-main {
      min-height: calc(100dvh - var(--shell-header-height, 52px));
      width: 100%;
      overflow-x: clip;
    }

    @media (max-width: 767px) {
      .shell-header {
        flex-wrap: nowrap;
      }

      .nav-toggle {
        display: inline-flex;
        margin-left: auto;
      }

      .shell-nav {
        display: none;
        width: 100%;
        flex-direction: column;
        align-items: stretch;
        gap: 4px;
        padding: 8px 0 4px;
        border-top: 1px solid rgba(255, 255, 255, 0.12);
      }

      .shell-nav.open {
        display: flex;
      }

      .shell-nav a {
        padding: 12px 8px;
        border-radius: 8px;
      }

      .shell-nav a:active,
      .shell-nav a:hover {
        background: rgba(255, 255, 255, 0.08);
      }
    }

    @media (min-width: 768px) and (max-width: 1023px) {
      .shell-nav {
        gap: 4px 12px;
      }

      .shell-nav a {
        font-size: 0.95rem;
      }
    }
  `],
  standalone: false,
})
export class AppComponent {
  navOpen = false;

  closeNav(): void {
    this.navOpen = false;
  }
}
