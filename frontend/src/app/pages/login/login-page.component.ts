import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'resto-login-page',
  template: `
    <div class="bounce">
      <p>Redirection vers la connexion…</p>
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
export class LoginPageComponent implements OnInit {
  constructor(private readonly auth: AuthService) {}

  ngOnInit(): void {
    this.auth.startKeycloakLogin();
  }
}
