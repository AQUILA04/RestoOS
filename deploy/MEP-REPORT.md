# Rapport MEP RestoOS — landing in-app, un host, Contabo

Date : 2026-09-09  
Slug : `restoos`  
Host public : https://restoos.optimizesolux.com  
Auth : https://auth.optimizesolux.com/realms/restoos

## Livré

### Skills
- `optimizesolux-mep` + `optimizesolux-product-landing` : CTA multi-tenant (Créer mon espace + Se connecter → Keycloak), landing in-app, pattern `LANDING_PUBLIC_URL` / `FRONTEND_PUBLIC_URL` / `API_PUBLIC_URL` same-origin.

### Frontend (RestoOS)
- Routes `/` et `/home` → landing marketing (bordeaux / laiton, Cormorant + Figtree)
- `/signup` → création d’espace (enseigne + user + établissement défaut)
- `/login` → bounce Keycloak (aucun formulaire)
- `/auth/callback` → échange OIDC
- `/admin/etablissements` → liste, rename, ajout, switch store
- E2E Playwright `frontend/e2e/specs/onboarding.spec.ts`

### Backend
- `POST /api/v1/auth/signup` + `POST /api/v1/auth/oidc/callback`
- `PATCH /api/v1/stores/{id}`
- Keycloak Admin client (optionnel)
- Invitations : notification-hub en prod, Mailpit/JavaMail en local

### Contabo
- Dockerfiles backend/frontend, `deploy/docker-compose.prod.yml` (Traefik Host + `/api` `/ws`)
- `.env.prod.example`, scripts init/deploy, `cd.yml` + publish GHCR dans `ci.yml`
- Doc secrets : `deploy/GITHUB-SECRETS-CONTABO.md` + `~/Documents/RestoOS-contabo-secrets.md`

### Corporate (`optimizesolux-web`)
- Carte app RestoOS → `https://restoos.optimizesolux.com`
- DNS-CONVENTION : A `restoos`, exception same-origin `/api`, emails produit

### Common-infra
- Realm `restoos` (redirects prod + local)
- Client hub `restoos` dans `notification-hub`
- Redis DB **9** / préfixe `restoos:`

## Non exécuté ici (bascule cloud / manuel)

| Action | Qui |
|--------|-----|
| Vérifs locales / e2e runtime | Cloud agent |
| DNS A `restoos` → `169.58.127.90` | Manuel Cloudflare |
| Email Routing `restoos@` / `contact.restoos@` / `support.restoos@` | Manuel Cloudflare |
| Deploy Cloudflare Pages corporate | Après push `optimizesolux-web` |
| `install.sh --force-update keycloak` | VPS common-infra |
| Promote Contabo | Actions → RestoOS CD → **promote** (pas auto) |

## Pattern URLs (référence)

| Config | Valeur |
|--------|--------|
| `LANDING_PUBLIC_URL` | `https://restoos.optimizesolux.com` |
| `FRONTEND_PUBLIC_URL` | `https://restoos.optimizesolux.com` |
| `API_PUBLIC_URL` | `https://restoos.optimizesolux.com/api` |
