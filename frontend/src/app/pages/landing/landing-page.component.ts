import { Component, OnDestroy, OnInit } from '@angular/core';
import { Title, Meta } from '@angular/platform-browser';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'resto-landing-page',
  templateUrl: './landing-page.component.html',
  styleUrls: ['./landing-page.component.css'],
  standalone: false,
})
export class LandingPageComponent implements OnInit, OnDestroy {
  private reducedMotion = false;

  constructor(
    private readonly auth: AuthService,
    private readonly title: Title,
    private readonly meta: Meta,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('RestoOS — La salle, la cuisine, le service');
    this.meta.updateTag({
      name: 'description',
      content:
        'Ouvrez votre espace RestoOS : caisse tactile, écran cuisine, carte et équipes — le même niveau d’exigence qu’en salle.',
    });
    this.meta.updateTag({ property: 'og:title', content: 'RestoOS — La salle, la cuisine, le service' });
    this.meta.updateTag({
      property: 'og:description',
      content: 'Créez votre espace et enregistrez vos premières commandes. Se connecter via votre compte.',
    });
    this.reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (!this.reducedMotion) {
      document.body.classList.add('landing-active');
    }
    requestAnimationFrame(() => this.observeReveals());
  }

  ngOnDestroy(): void {
    document.body.classList.remove('landing-active');
  }

  connect(): void {
    this.auth.startKeycloakLogin();
  }

  private observeReveals(): void {
    if (this.reducedMotion) {
      document.querySelectorAll('.reveal').forEach((el) => el.classList.add('in'));
      return;
    }
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) {
            e.target.classList.add('in');
            io.unobserve(e.target);
          }
        });
      },
      { threshold: 0.12 },
    );
    document.querySelectorAll('.reveal').forEach((el) => io.observe(el));
  }
}
