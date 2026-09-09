# RestoOS — Deploy Contabo

Stack prod : **PostgreSQL métier** + **API Spring Boot** + **frontend Angular** derrière **shared-traefik**.

Voir [GITHUB-SECRETS-CONTABO.md](./GITHUB-SECRETS-CONTABO.md) pour DNS, URLs et secrets.

## Layout VPS

```text
/opt/restoos/
  deploy/          # synchronisé depuis GitHub
  prod/.env        # secrets runtime (hors git)
  prod/releases/   # métadonnées de release
  init.sh          # bootstrap CD
```

## Host public

- `https://restoos.optimizesolux.com` → landing Angular + routes frontend
- `https://restoos.optimizesolux.com/api/*` → backend
- `https://restoos.optimizesolux.com/ws/*` → WebSocket

## Première mise en service

1. DNS A `restoos` → IP Contabo (DNS only)
2. Secrets GitHub + environment `prod`
3. CI a publié au moins une image GHCR backend + frontend
4. Actions → **RestoOS CD** → `workflow_dispatch` → **promote**

## Local

À la racine :

```bash
docker compose -f docker-compose.test.yml up -d
cd backend && mvn spring-boot:run
cd frontend && npm start
```
