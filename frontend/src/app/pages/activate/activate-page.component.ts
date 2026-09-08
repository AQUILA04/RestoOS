import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'resto-activate-page',
  templateUrl: './activate-page.component.html',
  styleUrls: ['./activate-page.component.css'],
  standalone: false,
})
export class ActivatePageComponent implements OnInit {
  status: 'pending' | 'success' | 'error' = 'pending';
  message = 'Activation en cours…';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly auth: AuthService,
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.status = 'error';
      this.message = 'Lien d’activation invalide (token manquant).';
      return;
    }
    this.auth.activate(token).subscribe({
      next: () => {
        this.status = 'success';
        this.message = 'Votre compte a été activé. Vous pouvez vous connecter.';
      },
      error: () => {
        this.status = 'error';
        this.message = 'Impossible d’activer le compte. Le lien est invalide ou déjà utilisé.';
      },
    });
  }
}
