import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'resto-signup-page',
  templateUrl: './signup-page.component.html',
  styleUrls: ['./signup-page.component.css'],
  standalone: false,
})
export class SignupPageComponent {
  organizationName = '';
  firstName = '';
  lastName = '';
  email = '';
  password = '';
  storeMode: 'single' | 'multi' = 'single';
  submitting = false;
  error = '';

  constructor(
    readonly auth: AuthService,
    private readonly router: Router,
  ) {}

  submit(): void {
    this.error = '';
    if (!this.organizationName.trim() || !this.email.trim() || !this.password) {
      this.error = 'Renseignez l’enseigne, l’e-mail et le mot de passe.';
      return;
    }
    if (this.password.length < 8) {
      this.error = 'Le mot de passe doit contenir au moins 8 caractères.';
      return;
    }
    this.submitting = true;
    const multiStore = this.storeMode === 'multi';
    this.auth
      .signup({
        organizationName: this.organizationName.trim(),
        firstName: this.firstName.trim(),
        lastName: this.lastName.trim(),
        email: this.email.trim(),
        password: this.password,
        multiStore,
      })
      .subscribe({
        next: (data) => {
          this.submitting = false;
          if (data?.needsLogin) {
            this.auth.startKeycloakLogin(this.email.trim());
            return;
          }
          const target =
            multiStore || (data?.storeCount ?? 1) > 1
              ? '/admin/etablissements'
              : '/admin/dashboard';
          this.router.navigateByUrl(target);
        },
        error: (err) => {
          this.submitting = false;
          const msg = err?.error?.message || err?.error?.data || err?.message;
          if (typeof msg === 'string' && /already|existe|exists|email/i.test(msg)) {
            this.error = 'Cet e-mail est déjà utilisé.';
          } else {
            this.error = typeof msg === 'string' ? msg : 'Impossible de créer l’espace.';
          }
        },
      });
  }
}
