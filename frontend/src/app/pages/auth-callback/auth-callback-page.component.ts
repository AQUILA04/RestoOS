import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'resto-auth-callback-page',
  template: `
    <div class="bounce">
      <p>{{ message }}</p>
    </div>
  `,
  styles: [
    `
      .bounce {
        min-height: 100dvh;
        display: grid;
        place-items: center;
        background: #1a0f12;
        color: #e8d4b0;
        font-family: Figtree, system-ui, sans-serif;
      }
    `,
  ],
  standalone: false,
})
export class AuthCallbackPageComponent implements OnInit {
  message = 'Connexion en cours…';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly auth: AuthService,
  ) {}

  ngOnInit(): void {
    const error = this.route.snapshot.queryParamMap.get('error');
    const code = this.route.snapshot.queryParamMap.get('code');
    if (error) {
      this.message = 'Connexion annulée.';
      return;
    }
    if (!code) {
      this.message = 'Code de connexion manquant.';
      return;
    }
    this.auth.completeKeycloakLogin(code).subscribe({
      next: (data) => {
        const stores = data?.storeCount ?? 1;
        this.router.navigateByUrl(stores > 1 ? '/admin/etablissements' : '/admin/dashboard');
      },
      error: () => {
        this.message = 'Impossible de finaliser la connexion.';
      },
    });
  }
}
