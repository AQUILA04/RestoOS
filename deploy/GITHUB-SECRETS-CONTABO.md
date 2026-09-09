# Secrets GitHub + DNS — RestoOS (OptimizeSolux Contabo)

Prérequis VPS : **shared-traefik** + **optimize-common-infra** avec realm `restoos` et client notification-hub `restoos`.

Auth produit : **Keycloak OIDC** (`OIDC_ISSUER_URI=https://auth.optimizesolux.com/realms/restoos`).
Notifications : **notification-hub**.

## 1. DNS Cloudflare

| Type | Name | Content | Proxy |
|------|------|---------|-------|
| A | `restoos` | `169.58.127.90` | DNS only |

Un seul host public :

- `https://restoos.optimizesolux.com` — landing + frontend
- `https://restoos.optimizesolux.com/api` — API

## 2. Secrets repo (Actions) + environment `prod`

| Secret | Valeur |
|--------|--------|
| `SSH_PRIVATE_KEY` | contenu de `~/.ssh/optimizesolux_vps_ed25519` |
| `PROD_SERVER_HOST` | `169.58.127.90` |
| `PROD_SERVER_USER` | `root` |
| `GHCR_USERNAME` | user GitHub |
| `GHCR_TOKEN` | PAT `read:packages` |
| `DB_ADMIN_USER` | `restoos_admin` |
| `DB_APP_USER` | `restoos_app` |
| `PROD_DB_PASSWORD` | mot de passe fort PostgreSQL |
| `PROD_DB_APP_PASSWORD` | mot de passe fort app |
| `PROD_DB_NAME` | `restoos` |
| `PROD_APP_HOSTNAME` | `restoos.optimizesolux.com` |
| `JWT_SECRET` | secret HMAC fort |
| `OIDC_ISSUER_URI` | `https://auth.optimizesolux.com/realms/restoos` |
| `OIDC_CLIENT_ID` | `restoos-frontend` |
| `KEYCLOAK_URL` | `https://auth.optimizesolux.com` |
| `KEYCLOAK_ADMIN_CLIENT_ID` | `restoos-backend` |
| `KEYCLOAK_ADMIN_CLIENT_SECRET` | secret confidential client |
| `LANDING_PUBLIC_URL` | `https://restoos.optimizesolux.com` |
| `FRONTEND_PUBLIC_URL` | `https://restoos.optimizesolux.com` |
| `API_PUBLIC_URL` | `https://restoos.optimizesolux.com/api` |
| `REDIS_PASSWORD` | mot de passe Redis common-infra |
| `OPTIMIZE_NOTIFICATION_HUB_OAUTH2_CLIENT_SECRET` | secret Keycloak realm `notification-hub` client `restoos` |

Créer aussi l’**environment** GitHub Actions nommé `prod`.

## 3. Hosts runtime

| URL | Rôle |
|-----|------|
| https://restoos.optimizesolux.com | Landing + SPA + API same-origin |
| https://auth.optimizesolux.com/realms/restoos | Keycloak realm produit |
| https://notification-api.optimizesolux.com | notification-hub API |

## 4. Pipelines

| Workflow | Trigger |
|----------|---------|
| **CI** | push / PR → quality + build + e2e + publish GHCR |
| **CD** | CI success sur `release/**` ou `workflow_dispatch` → SSH Contabo |

## 5. Source de vérité

- Runtime Docker = `deploy/`
- Secrets hors git : `/opt/restoos/prod/.env`
- Template : `deploy/.env.prod.example`
- Redis DB index réservé : `9` (`restoos:`)
