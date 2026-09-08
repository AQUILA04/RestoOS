---
title: RestoOS — Spécification fonctionnelle & technique
status: draft
created: 2026-09-08
updated: 2026-09-08
---

# RestoOS — Spécification fonctionnelle & technique

**Version : 1.0 — MVP + architecture V2**

---

# 1. Vision du produit

## 1.1 Objectif

RestoOS est une plateforme SaaS permettant à une enseigne de restauration de gérer depuis une application centralisée :

* ses établissements ;
* ses employés ;
* son catalogue ;
* ses prix ;
* ses disponibilités locales ;
* son plan de salle ;
* ses commandes ;
* la préparation en cuisine ;
* la livraison/service ;
* l'état de paiement.

Le système doit permettre à un employé de travailler dans plusieurs établissements sans recréer son compte.

Le système doit également garantir une **isolation stricte des données entre organisations**, y compris lorsqu'elles partagent la même infrastructure PostgreSQL.

---

# 2. Périmètre fonctionnel

## 2.1 MVP

Le MVP comprend les modules suivants :

| Module                  |  MVP |
| ----------------------- | ---: |
| Multi-tenant            |    ✅ |
| Organisations           |    ✅ |
| Établissements          |    ✅ |
| Utilisateurs/employés   |    ✅ |
| Rôles et permissions    |    ✅ |
| Affectation multi-sites |    ✅ |
| Catalogue central       |    ✅ |
| Catégories              |    ✅ |
| Produits                |    ✅ |
| Modificateurs/options   |    ✅ |
| Prix locaux             |    ✅ |
| Rupture / 86            |    ✅ |
| Tables                  |    ✅ |
| POS                     |    ✅ |
| Commandes               |    ✅ |
| KDS                     |    ✅ |
| Service/livraison       |    ✅ |
| Paiement déclaratif     |    ✅ |
| Historique/audit        |    ✅ |
| Dashboard opérationnel  |    ✅ |
| Analytics avancés       | ❌ V2 |
| Click & Collect         | ❌ V2 |
| Livraison externe       | ❌ V2 |
| Paiement électronique   | ❌ V2 |

---

# 3. Architecture fonctionnelle

```text
                         ┌─────────────────────┐
                         │     RestoOS SaaS    │
                         └──────────┬──────────┘
                                    │
              ┌─────────────────────┼─────────────────────┐
              │                     │                     │
              ▼                     ▼                     ▼
       Administration             POS                   KDS
              │                     │                     │
       ┌──────┼──────┐              │                     │
       │      │      │              │                     │
    Tenant  Sites  Users            │                     │
       │             │              │                     │
       └─────────────┴──────┐       │                     │
                            ▼       ▼                     ▼
                         Catalogue → Commande → Cuisine
                                      │
                                      ▼
                                  Livraison
                                      │
                                      ▼
                                  Paiement
```

---

# 4. Modèle organisationnel

La hiérarchie principale est :

```text
Platform
   │
   ├── Organization
   │       │
   │       ├── Store
   │       │      ├── Tables
   │       │      ├── Local catalogue
   │       │      └── Orders
   │       │
   │       ├── Employees
   │       │
   │       └── Central catalogue
   │
   └── Organization
```

## 4.1 Organization

Une `Organization` représente :

* une franchise ;
* une chaîne ;
* un restaurant indépendant ;
* une société exploitante.

Exemples :

```text
Burger Factory
 ├── Paris Centre
 ├── Paris Nord
 └── Lyon Part-Dieu
```

Une autre organisation :

```text
Pizza Express
 ├── Lomé Centre
 ├── Lomé Tokoin
 └── Cotonou
```

Ces deux organisations peuvent partager la même base PostgreSQL mais **ne doivent jamais pouvoir accéder aux données de l'autre**.

---

# 5. Multi-tenancy

## 5.1 Principe

Le modèle retenu est :

> **Shared database + shared schema + PostgreSQL Row-Level Security**

Toutes les données métier appartenant à une organisation possèdent :

```sql
organization_id UUID NOT NULL
```

Les données spécifiques à un établissement possèdent également :

```sql
store_id UUID
```

---

# 6. Contexte PostgreSQL

La connexion applicative doit établir le contexte avant chaque transaction :

```sql
SET LOCAL app.current_org_id = '...';
SET LOCAL app.current_store_id = '...';
```

Exemple :

```sql
CREATE POLICY organization_isolation
ON orders
USING (
    organization_id =
    current_setting('app.current_org_id')::uuid
);
```

Pour les données propres à un établissement :

```sql
CREATE POLICY store_isolation
ON orders
USING (
    organization_id =
        current_setting('app.current_org_id')::uuid
    AND
    store_id =
        current_setting('app.current_store_id')::uuid
);
```

## 6.1 Règle fondamentale

L'application **ne doit jamais considérer le frontend comme une source fiable de `organization_id` ou `store_id`.**

Le contexte doit être dérivé :

```text
JWT
 ↓
User
 ↓
Membership
 ↓
Organization
 ↓
Store
```

Le client peut demander :

```http
GET /api/orders
```

mais pas imposer :

```http
GET /api/orders?organizationId=OTHER_TENANT
```

pour contourner la sécurité.

---

# 7. Utilisateurs et employés

## 7.1 User

Un utilisateur est global à RestoOS.

```text
User
 ├── id
 ├── email
 ├── name
 └── status
```

Il peut appartenir à plusieurs organisations ou établissements selon le modèle de droits.

---

# 8. Membership

Une table de type :

```text
organization_memberships
```

permet de représenter :

```text
User A
 ├── Organization X
 │      ├── Store A
 │      └── Store B
 │
 └── Organization Y
        └── Store C
```

Pour le MVP, il est préférable de distinguer :

### Organisation-level role

```text
OWNER
ADMIN
```

### Store-level role

```text
STORE_MANAGER
CASHIER
WAITER
KITCHEN
```

---

# 9. Authentification

Je recommande :

```text
Angular
   ↓
OIDC
   ↓
Identity Provider
   ↓
JWT
   ↓
Spring Security
```

Le backend doit valider :

* signature ;
* issuer ;
* audience ;
* expiration ;
* subject ;
* rôles.

Exemple de claims :

```json
{
  "sub": "user-123",
  "roles": [
    "STORE_MANAGER",
    "CASHIER"
  ]
}
```

Le contexte métier est ensuite déterminé côté serveur.

---

# 10. Connexion rapide par PIN

Pour le personnel opérationnel, le MVP peut proposer :

```text
Écran POS

┌─────────────────────────┐
│      RestoOS POS        │
│                         │
│   Sélection utilisateur │
│                         │
│ [ Alice ] [ Bob ]       │
│ [ Marc  ] [ Sarah ]     │
│                         │
│       ● ● ● ●           │
└─────────────────────────┘
```

Le PIN ne doit **jamais être stocké en clair**.

Stockage :

```text
Argon2id / BCrypt
```

Le PIN est associé au membership/store et non utilisé comme identifiant global.

---

# 11. Catalogue

Le catalogue possède deux niveaux.

```text
Catalogue central
        │
        ▼
Configuration établissement
        │
        ├── actif
        ├── prix
        └── disponibilité
```

---

# 12. Catégories

Exemple :

```text
Burgers
 ├── Classic Burger
 ├── Cheese Burger
 └── Double Burger

Boissons
 ├── Coca-Cola
 ├── Fanta
 └── Eau

Desserts
 ├── Tiramisu
 └── Glace
```

Table :

```text
categories
```

Champs principaux :

```text
id
organization_id
name
description
display_order
active
created_at
updated_at
```

---

# 13. Produits

```text
products
```

Exemple :

```text
Cheese Burger
Prix catalogue : 7.50 €
```

Champs :

```text
id
organization_id
category_id
name
description
image_url
base_price
tax_rate
display_order
active
```

---

# 14. Modificateurs

Un produit peut avoir des options.

Exemple :

```text
Burger
 ├── Cuisson
 │    ├── Saignant
 │    ├── À point
 │    └── Bien cuit
 │
 ├── Sauce
 │    ├── BBQ
 │    └── Andalouse
 │
 └── Suppléments
      ├── Fromage +1€
      └── Bacon +2€
```

Modèle :

```text
modifier_groups
modifier_options
product_modifier_groups
```

---

# 15. Surcharge locale

Un établissement peut modifier le catalogue sans modifier le catalogue central.

Exemple :

```text
Catalogue central
Cheese Burger = 7.50 €

Restaurant Lyon
Cheese Burger = 8.00 €
```

ou :

```text
Restaurant Paris

Cheese Burger
status = OUT_OF_STOCK
```

La configuration locale peut contenir :

```text
store_products
------------------
store_id
product_id
price_override
available
display_order
```

---

# 16. Règle de résolution du prix

Le prix final est déterminé par :

```text
IF local_price exists
    use local_price
ELSE
    use product.base_price
```

Même principe pour la disponibilité :

```text
Central product active
       +
Store product available
       =
Produit vendable
```

---

# 17. Gestion "86"

Le manager peut désactiver instantanément un produit :

```text
☑ Disponible
☐ Rupture
```

Exemple :

```text
Cheese Burger
🔴 ÉPUISÉ
```

Conséquences :

* le produit disparaît du POS ;
* il ne peut plus être ajouté à une nouvelle commande ;
* les commandes existantes ne sont pas modifiées ;
* le KDS conserve les commandes déjà envoyées.

---

# 18. Tables

Chaque établissement possède son propre plan de salle.

Exemple :

```text
Salle
 ├── Table 1
 ├── Table 2
 ├── Table 3
 └── Table 4

Terrasse
 ├── Terrasse 1
 ├── Terrasse 2
 └── Terrasse 3
```

Table :

```text
tables
```

Champs :

```text
id
store_id
name
zone
capacity
status
display_order
active
```

Status :

```text
AVAILABLE
OCCUPIED
RESERVED
OUT_OF_SERVICE
```

---

# 19. POS

Le POS constitue le cœur opérationnel du MVP.

## 19.1 Écran principal

```text
┌───────────────────────────────────────────────────┐
│ RestoOS                         Table 12   👤 Alice│
├───────────────┬───────────────────────┬───────────┤
│ CATÉGORIES    │ PRODUITS              │ PANIER    │
│               │                       │           │
│ Burgers       │ Cheese Burger         │ 2 Burger │
│ Menus         │ Classic Burger        │ 1 Fanta  │
│ Boissons      │ Double Burger         │           │
│ Desserts      │                       │ Total     │
│               │                       │ 19.50 €   │
└───────────────┴───────────────────────┴───────────┘
```

---

# 20. Mode de consommation

Lors de la création :

```text
Type :

[ SUR PLACE ]

[ À EMPORTER ]
```

## Sur place

Une table peut être sélectionnée.

```text
Table 12
```

La règle métier recommandée est :

```text
DINE_IN
    table_id obligatoire
```

Si l'on souhaite autoriser une commande "sur place sans table", prévoir explicitement :

```text
DINE_IN_COUNTER
```

plutôt que de rendre `table_id` ambigu.

---

# 21. Commande

Structure :

```text
Order
 ├── OrderItem
 │     ├── Product
 │     ├── Quantity
 │     ├── UnitPrice
 │     ├── Modifiers
 │     └── Note
 │
 ├── Customer
 ├── Table
 └── Payment
```

---

# 22. Numéro de commande

Chaque établissement possède un numéro lisible.

Exemple :

```text
#1042
#1043
#1044
```

Il faut distinguer :

```text
id UUID
```

de :

```text
order_number INTEGER
```

Le UUID assure l'identité technique.

Le numéro sert à l'exploitation humaine.

---

# 23. Création d'une commande

Flux :

```text
POS
 ↓
Choix mode
 ↓
Choix table
 ↓
Ajout produits
 ↓
Options
 ↓
Notes
 ↓
Validation
 ↓
Commande créée
 ↓
Transmission KDS
```

---

# 24. Validation de commande

Lors de la validation :

1. vérifier que le produit existe ;
2. vérifier qu'il appartient à l'organisation ;
3. vérifier qu'il est disponible dans le store ;
4. recalculer le prix côté backend ;
5. calculer les taxes ;
6. calculer le total ;
7. créer la commande ;
8. créer les lignes ;
9. enregistrer les modificateurs ;
10. publier un événement ;
11. transmettre au KDS.

Le prix envoyé par Angular ne doit **jamais être considéré comme fiable**.

---

# 25. Snapshot des données

Point important.

Une commande doit conserver le prix et le libellé au moment de la vente.

Exemple :

```text
product.name
    =
"Cheese Burger"

order_item.product_name
    =
"Cheese Burger"
```

Si le produit devient demain :

```text
"Cheese Burger XL"
```

les anciennes commandes doivent rester historiquement correctes.

Même principe pour :

* prix ;
* TVA ;
* options ;
* nom produit.

---

# 26. États de commande

Je recommande de distinguer plusieurs dimensions plutôt qu'un seul statut.

## 26.1 Statut opérationnel

```text
CREATED
   ↓
SENT_TO_KITCHEN
   ↓
PREPARING
   ↓
READY
   ↓
DELIVERED
   ↓
CLOSED
```

Éventuellement :

```text
CANCELLED
```

---

# 27. Statut financier

Indépendant du statut opérationnel :

```text
UNPAID
PAID
```

Donc :

```text
Order status     = READY
Payment status   = UNPAID
```

est parfaitement valide.

---

# 28. Matrice de workflow

| Opérationnel    | Paiement | Signification          |
| --------------- | -------- | ---------------------- |
| CREATED         | UNPAID   | commande créée         |
| SENT_TO_KITCHEN | UNPAID   | cuisine informée       |
| PREPARING       | UNPAID   | préparation en cours   |
| READY           | UNPAID   | prête mais non réglée  |
| READY           | PAID     | prête et réglée        |
| DELIVERED       | UNPAID   | livrée mais non réglée |
| DELIVERED       | PAID     | livrée et réglée       |
| CLOSED          | PAID     | terminée               |
| CANCELLED       | UNPAID   | annulée                |

Le système doit idéalement interdire :

```text
CLOSED + UNPAID
```

sauf procédure exceptionnelle explicitement autorisée.

---

# 29. Pourquoi séparer ces statuts ?

Parce que :

```text
ORDER_STATUS
```

et :

```text
PAYMENT_STATUS
```

évoluent indépendamment.

Un client peut :

```text
payer
    ↓
attendre la préparation
```

ou :

```text
manger
    ↓
payer après
```

Le modèle doit donc le supporter nativement.

---

# 30. KDS

Le KDS reçoit les commandes dès qu'elles sont envoyées.

Interface :

```text
┌─────────────────────────────────────────────┐
│ KDS                     Cuisine     12:45    │
├────────────┬────────────┬───────────────────┤
│ #1042      │ #1043      │ #1044             │
│ Table 12   │ EMPORTER   │ Table 8            │
│            │            │                   │
│ 2x Burger  │ 1x Pizza   │ 2x Burger         │
│ 1x Fanta   │ 1x Cola    │ + fromage         │
│            │            │                   │
│ [PRÉPARER] │ [PRÉPARER] │ [PRÉPARER]       │
└────────────┴────────────┴───────────────────┘
```

---

# 31. Temps réel

Le système doit éviter :

```text
KDS → polling toutes les 5 secondes
```

et utiliser :

```text
POS
 ↓
Backend
 ↓
Event
 ↓
WebSocket
 ↓
KDS
```

Objectif :

```text
< 1 seconde
```

entre validation POS et affichage KDS dans des conditions normales.

---

# 32. WebSocket

Exemple de destination logique :

```text
/store/{storeId}/kitchen
```

Événement :

```json
{
  "event": "ORDER_CREATED",
  "orderId": "uuid",
  "orderNumber": 1042,
  "storeId": "uuid"
}
```

Le KDS reçoit ensuite les données autorisées.

---

# 33. Important : sécurité WebSocket

Il ne faut pas considérer :

```text
/store/123/kitchen
```

comme une autorisation.

Le backend doit vérifier :

```text
JWT
 ↓
User
 ↓
Membership
 ↓
Store
```

avant d'autoriser la souscription.

---

# 34. Workflow KDS

```text
SENT_TO_KITCHEN
       │
       ▼
   PREPARING
       │
       ▼
      READY
```

Actions :

```text
[Prendre en charge]
[Marquer prêt]
```

---

# 35. Temps de préparation

Chaque changement d'état doit enregistrer :

```text
created_at
sent_to_kitchen_at
preparing_at
ready_at
delivered_at
closed_at
```

Cela permettra V2 de calculer :

```text
Temps attente cuisine
Temps préparation
Temps total
```

---

# 36. Service

Écran :

```text
COMMANDES PRÊTES

#1042
Table 12
2 produits

[ LIVRÉ ]

#1043
À EMPORTER
1 produit

[ REMIS AU CLIENT ]
```

---

# 37. À emporter

Pour :

```text
TAKEAWAY
```

le numéro de commande devient l'identifiant client principal.

Exemple :

```text
Commande #1045 prête
```

L'interface peut afficher :

```text
#1045 — PRÊTE
```

---

# 38. Livraison/service sur place

Pour `DINE_IN` :

```text
Order
 └── Table 12
```

L'information doit apparaître partout où nécessaire :

```text
KDS
Service
Caisse
Manager
```

---

# 39. Encaissement

Le MVP ne communique avec aucun TPE.

Le paiement est déclaratif.

```text
UNPAID
   ↓
[ MARQUER PAYÉ ]
   ↓
PAID
```

---

# 40. Encaissement avant/après repas

Les deux scénarios sont supportés.

### Comptoir

```text
Commande
 ↓
Paiement
 ↓
Cuisine
 ↓
Préparation
```

### Table

```text
Commande
 ↓
Cuisine
 ↓
Service
 ↓
Paiement
```

Le système ne doit donc **pas conditionner `READY` ou `DELIVERED` au paiement**.

---

# 41. Audit du paiement

Le passage :

```text
UNPAID → PAID
```

doit être audité.

Exemple :

```text
Payment status changed

Order: #1042
From: UNPAID
To: PAID
User: Alice
Timestamp: 2026-09-08 12:43:11
```

---

# 42. Annulation

Une commande ne doit pas être supprimée physiquement.

Utiliser :

```text
CANCELLED
```

avec :

```text
cancelled_at
cancelled_by
cancellation_reason
```

L'historique doit rester disponible.

---

# 43. Architecture technique

Je recommande :

```text
                    Internet / LAN
                         │
                         ▼
                    Traefik
                         │
              ┌──────────┴──────────┐
              │                     │
              ▼                     ▼
          Angular SPA          Spring Boot API
                                    │
                     ┌──────────────┼───────────────┐
                     │              │               │
                     ▼              ▼               ▼
                PostgreSQL       Redis         Message Broker*
```

`Message Broker` peut être absent du MVP initial.

---

# 44. Stack recommandée

## Backend

```text
Java 21
Spring Boot 3.5+
Spring Security
Spring Data JPA
Hibernate
Flyway/Liquibase
WebSocket/STOMP
Bean Validation
PostgreSQL
```

Compte tenu de votre stack, **Spring Boot + PostgreSQL + Angular** est particulièrement adapté à ce projet.

## Frontend

```text
Angular
TypeScript
RxJS
Angular Signals
Tailwind CSS
PWA
```

---

# 45. API REST

Architecture :

```text
/api/v1/auth
/api/v1/organizations
/api/v1/stores
/api/v1/users
/api/v1/catalog
/api/v1/categories
/api/v1/products
/api/v1/tables
/api/v1/orders
/api/v1/kitchen
/api/v1/payments
```

---

# 46. Exemple API commandes

### Créer

```http
POST /api/v1/orders
```

```json
{
  "type": "DINE_IN",
  "tableId": "uuid",
  "items": [
    {
      "productId": "uuid",
      "quantity": 2,
      "modifiers": [
        {
          "optionId": "uuid"
        }
      ],
      "note": "Sans oignons"
    }
  ]
}
```

Le backend retourne :

```json
{
  "id": "uuid",
  "orderNumber": 1042,
  "status": "SENT_TO_KITCHEN",
  "paymentStatus": "UNPAID",
  "total": 19.50
}
```

---

# 47. Endpoints principaux

### Catalogue

```http
GET    /api/v1/catalog
POST   /api/v1/products
PUT    /api/v1/products/{id}
DELETE /api/v1/products/{id}
```

### Catalogue local

```http
GET /api/v1/stores/{storeId}/products
PUT /api/v1/stores/{storeId}/products/{productId}
```

### Rupture

```http
POST /api/v1/stores/{storeId}/products/{productId}/availability
```

### Tables

```http
GET  /api/v1/stores/{storeId}/tables
POST /api/v1/stores/{storeId}/tables
PUT  /api/v1/tables/{id}
```

### Commandes

```http
POST /api/v1/orders
GET  /api/v1/orders/{id}
GET  /api/v1/orders
POST /api/v1/orders/{id}/cancel
```

### KDS

```http
GET  /api/v1/kitchen/orders
POST /api/v1/kitchen/orders/{id}/start
POST /api/v1/kitchen/orders/{id}/ready
```

### Service

```http
POST /api/v1/orders/{id}/deliver
```

### Paiement

```http
POST /api/v1/orders/{id}/payment/mark-paid
```

---

# 48. Modèle de données

Vue simplifiée :

```text
organizations
      │
      ├───────────────┐
      │               │
      ▼               ▼
stores           users/memberships
      │
      ├────── tables
      │
      ├────── store_products
      │
      └────── orders
                  │
                  ├──── order_items
                  │          │
                  │          └── order_item_modifiers
                  │
                  └──── payments
```

Catalogue :

```text
organizations
      │
      └── categories
             │
             └── products
                    │
                    └── modifier_groups
                           │
                           └── modifier_options
```

---

# 49. Tables PostgreSQL principales

## organizations

```sql
id UUID PRIMARY KEY
name VARCHAR(150)
slug VARCHAR(100)
status VARCHAR(30)
created_at TIMESTAMP
updated_at TIMESTAMP
```

## stores

```sql
id UUID PRIMARY KEY
organization_id UUID NOT NULL
name VARCHAR(150)
code VARCHAR(50)
timezone VARCHAR(50)
currency VARCHAR(3)
status VARCHAR(30)
created_at TIMESTAMP
updated_at TIMESTAMP
```

---

# 50. users

```sql
id UUID PRIMARY KEY
email VARCHAR(255)
first_name VARCHAR(100)
last_name VARCHAR(100)
status VARCHAR(30)
created_at TIMESTAMP
updated_at TIMESTAMP
```

---

# 51. memberships

```sql
id UUID PRIMARY KEY
user_id UUID NOT NULL
organization_id UUID NOT NULL
role VARCHAR(50)
status VARCHAR(30)
created_at TIMESTAMP
```

Puis :

```text
membership_stores
```

pour les affectations multi-sites.

---

# 52. products

```sql
id UUID PRIMARY KEY
organization_id UUID NOT NULL
category_id UUID
name VARCHAR(200)
description TEXT
base_price NUMERIC(12,2)
tax_rate NUMERIC(5,2)
active BOOLEAN
created_at TIMESTAMP
updated_at TIMESTAMP
```

---

# 53. store_products

```sql
id UUID PRIMARY KEY
organization_id UUID NOT NULL
store_id UUID NOT NULL
product_id UUID NOT NULL

price_override NUMERIC(12,2)
available BOOLEAN NOT NULL DEFAULT TRUE

created_at TIMESTAMP
updated_at TIMESTAMP
```

Contrainte :

```text
UNIQUE(store_id, product_id)
```

---

# 54. orders

```sql
id UUID PRIMARY KEY

organization_id UUID NOT NULL
store_id UUID NOT NULL

order_number BIGINT NOT NULL

type VARCHAR(30)
status VARCHAR(30)
payment_status VARCHAR(30)

table_id UUID NULL

subtotal NUMERIC(12,2)
tax_total NUMERIC(12,2)
discount_total NUMERIC(12,2)
total NUMERIC(12,2)

created_by UUID

created_at TIMESTAMP
sent_to_kitchen_at TIMESTAMP
preparing_at TIMESTAMP
ready_at TIMESTAMP
delivered_at TIMESTAMP
closed_at TIMESTAMP
cancelled_at TIMESTAMP
```

---

# 55. order_items

```sql
id UUID PRIMARY KEY

order_id UUID NOT NULL

product_id UUID NOT NULL

product_name VARCHAR(200) NOT NULL

quantity INTEGER NOT NULL

unit_price NUMERIC(12,2) NOT NULL

tax_rate NUMERIC(5,2)

subtotal NUMERIC(12,2)

note TEXT
```

---

# 56. order_item_modifiers

```sql
id UUID PRIMARY KEY
order_item_id UUID NOT NULL
modifier_group_name VARCHAR(150)
option_id UUID
option_name VARCHAR(150)
price_delta NUMERIC(12,2)
```

Encore une fois, le nom et le prix sont des **snapshots historiques**.

---

# 57. payments

Même si le MVP n'intègre pas de monétique, je recommande de créer le modèle :

```sql
payments
```

avec :

```text
id
organization_id
store_id
order_id
status
amount
method
recorded_by
recorded_at
```

Méthodes possibles :

```text
CASH
CARD
MOBILE_MONEY
OTHER
```

Le système n'effectue aucun appel externe.

---

# 58. Pourquoi créer `payments` malgré le statut binaire ?

Parce que le futur paiement électronique pourra être ajouté sans casser le modèle.

MVP :

```text
Payment
status = PAID
method = CASH
```

V2 :

```text
Payment
status = PAID
method = STRIPE
provider_transaction_id = ...
```

---

# 59. Audit log

Table :

```text
audit_logs
```

Exemple :

```text
id
organization_id
store_id
user_id
entity_type
entity_id
action
old_value
new_value
created_at
```

Actions :

```text
ORDER_CREATED
ORDER_STATUS_CHANGED
PAYMENT_MARKED_PAID
PRODUCT_DISABLED
PRICE_CHANGED
USER_ASSIGNED_STORE
ORDER_CANCELLED
```

---

# 60. RLS — architecture recommandée

Il faut activer RLS sur **toutes les tables tenantisées**.

Exemple :

```sql
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;

ALTER TABLE orders FORCE ROW LEVEL SECURITY;
```

Puis :

```sql
CREATE POLICY orders_tenant_policy
ON orders
USING (
    organization_id =
    current_setting('app.current_org_id', true)::uuid
);
```

Pour une table store :

```sql
CREATE POLICY orders_store_policy
ON orders
USING (
    organization_id =
      current_setting('app.current_org_id', true)::uuid
    AND
    store_id =
      current_setting('app.current_store_id', true)::uuid
);
```

---

# 61. Attention au rôle PostgreSQL

Le rôle utilisé par Spring ne doit pas pouvoir contourner RLS via des privilèges superuser.

Il faut notamment éviter que le rôle applicatif possède des capacités permettant de bypasser les policies.

C'est un point critique du projet.

---

# 62. Transaction et RLS

Le backend doit faire :

```text
BEGIN
   ↓
SET LOCAL app.current_org_id
   ↓
SET LOCAL app.current_store_id
   ↓
requêtes JPA
   ↓
COMMIT
```

Il ne faut surtout pas faire un :

```sql
SET app.current_org_id = ...
```

globalement sur une connexion de pool sans gestion rigoureuse, car une connexion peut être réutilisée par un autre tenant.

---

# 63. Couche backend

Je recommande une architecture hexagonale/modulaire plutôt qu'un simple gros package Spring.

```text
com.restoos
│
├── organization
├── identity
├── store
├── catalog
├── tablemanagement
├── order
├── kitchen
├── payment
├── audit
└── shared
```

Chaque module :

```text
controller
application
domain
infrastructure
```

---

# 64. Exemple

```text
order
├── application
│   ├── CreateOrderUseCase
│   ├── SendOrderToKitchenUseCase
│   ├── MarkOrderReadyUseCase
│   └── DeliverOrderUseCase
│
├── domain
│   ├── Order
│   ├── OrderItem
│   ├── OrderStatus
│   └── PaymentStatus
│
├── infrastructure
│   ├── OrderRepository
│   └── JpaOrderRepository
│
└── api
    └── OrderController
```

---

# 65. State Machine

Il est préférable de centraliser les transitions.

Exemple :

```java
CREATED -> SENT_TO_KITCHEN
SENT_TO_KITCHEN -> PREPARING
PREPARING -> READY
READY -> DELIVERED
DELIVERED -> CLOSED
```

Transitions interdites :

```text
CREATED -> READY
CREATED -> DELIVERED
CLOSED -> PREPARING
CANCELLED -> READY
```

sauf workflows explicitement prévus.

---

# 66. Command Pattern

Pour les transitions métier :

```text
SendOrderToKitchen
StartPreparation
MarkOrderReady
DeliverOrder
MarkOrderPaid
CancelOrder
```

Chaque commande métier :

1. vérifie les permissions ;
2. vérifie l'état actuel ;
3. modifie l'agrégat ;
4. persiste ;
5. publie un événement ;
6. écrit l'audit.

---

# 67. Événements métier

Exemples :

```text
OrderCreated
OrderSentToKitchen
OrderPreparationStarted
OrderReady
OrderDelivered
OrderPaid
OrderCancelled
```

Cela facilitera fortement V2.

---

# 68. Event flow

```text
CreateOrder
      │
      ▼
 PostgreSQL
      │
      ▼
OrderCreated
      │
      ├───────────────► WebSocket ──► KDS
      │
      └───────────────► Audit
```

---

# 69. Cohérence transactionnelle

Pour le MVP, il n'est pas nécessaire d'introduire Kafka immédiatement.

On peut utiliser :

```text
Spring Transaction
       +
PostgreSQL
       +
Transactional Outbox
```

Exemple :

```text
orders
outbox_events
```

Dans **la même transaction** :

```text
INSERT order
INSERT outbox_event
COMMIT
```

Puis un publisher transmet l'événement au WebSocket/broker.

Cela évite :

```text
Commande créée en DB
mais événement perdu
```

---

# 70. Cache

Redis peut être utilisé pour :

* sessions techniques ;
* données catalogue fréquemment consultées ;
* présence KDS ;
* rate limiting ;
* cache de configuration.

Mais **les commandes et paiements ne doivent pas dépendre de Redis comme source de vérité**.

PostgreSQL reste la source de vérité.

---

# 71. Frontend Angular

Architecture :

```text
src/app
│
├── core
│   ├── auth
│   ├── http
│   ├── websocket
│   └── tenant-context
│
├── features
│   ├── pos
│   ├── kitchen
│   ├── orders
│   ├── catalog
│   ├── tables
│   ├── payment
│   └── administration
│
└── shared
```

---

# 72. POS UX

Objectif :

> commande standard créée et envoyée en cuisine en moins de 20 secondes.

Cela impose une interface :

* tactile ;
* peu profonde ;
* très peu de modales ;
* navigation clavier possible ;
* raccourcis ;
* gros boutons ;
* catégories persistantes ;
* panier toujours visible.

---

# 73. KDS UX

Le KDS doit être utilisable avec :

* écran tactile ;
* souris ;
* clavier.

Les commandes doivent être classées par :

```text
created_at ASC
```

avec indication visuelle du temps d'attente.

Exemple :

```text
#1042
⏱ 00:08
```

Puis :

```text
⏱ 00:17
```

---

# 74. Gestion des commandes simultanées

Deux employés peuvent agir simultanément sur la même commande.

Le backend doit empêcher les incohérences via :

* optimistic locking ;
* `version` Hibernate ;
* transitions transactionnelles.

Exemple :

```text
Order.version = 5
```

Deux clients lisent version 5.

Le premier sauvegarde :

```text
5 → 6
```

Le second reçoit :

```text
OptimisticLockException
```

---

# 75. Idempotence

Les opérations critiques doivent être idempotentes.

Particulièrement :

```text
POST /orders
POST /payment/mark-paid
POST /deliver
```

Exemple :

```http
Idempotency-Key: 7f3e...
```

Cela évite une double commande lorsqu'un terminal perd momentanément la connexion.

---

# 76. Mode offline

Pour un POS de restaurant, c'est une fonctionnalité fortement recommandée même si elle n'est pas totalement développée au MVP.

Minimum :

```text
PWA
+
IndexedDB
+
queue locale
```

En cas de coupure :

```text
POS
 ↓
Local queue
 ↓
Connexion rétablie
 ↓
Synchronisation
```

Il faudra toutefois définir précisément les règles de conflit avant d'activer ce mode en production.

---

# 77. Sécurité

## Authentification

```text
OIDC
JWT
HTTPS
```

## Autorisation

```text
RBAC
+
Organization membership
+
Store membership
+
PostgreSQL RLS
```

Les quatre couches sont complémentaires.

---

# 78. Défense en profondeur

Une requête doit passer :

```text
JWT
 ↓
Spring Security
 ↓
Role authorization
 ↓
Organization authorization
 ↓
Store authorization
 ↓
PostgreSQL RLS
```

Même si un bug existe dans une couche applicative, RLS constitue la dernière barrière.

---

# 79. RGPD / données personnelles

Pour le MVP, limiter les données client.

Éviter de stocker inutilement :

* adresse ;
* téléphone ;
* email ;
* données personnelles.

Pour une commande en salle :

```text
customer_id = null
```

suffit généralement.

La V2 pourra introduire un modèle client.

---

# 80. Observabilité

Le système doit produire :

### Logs

```text
JSON structured logs
```

avec :

```text
timestamp
level
service
trace_id
organization_id
store_id
user_id
```

Attention à ne jamais logger :

* PIN ;
* token JWT ;
* informations sensibles.

---

# 81. Monitoring

Métriques :

```text
orders_created_total
orders_ready_total
orders_cancelled_total
orders_paid_total

kitchen_preparation_duration
order_total_duration

websocket_connections
websocket_delivery_latency

http_request_duration
http_error_total
```

---

# 82. Health checks

Spring Actuator :

```text
/actuator/health
```

avec checks :

```text
PostgreSQL
Redis
Broker
```

si présents.

---

# 83. Performance

Objectifs MVP :

| Indicateur            |         Objectif |
| --------------------- | ---------------: |
| Création commande API |         < 300 ms |
| Affichage KDS         |            < 1 s |
| Recherche catalogue   |         < 200 ms |
| POS interaction       | < 100 ms côté UI |
| 95e percentile API    |         < 500 ms |
| disponibilité         |         ≥ 99,9 % |

Les valeurs devront être validées par des tests de charge réels.

---

# 84. Charge cible initiale

Je définirais une première cible :

```text
1 000 organisations
10 000 établissements
50 000 utilisateurs
```

mais surtout :

```text
100 commandes/minute/établissement
```

comme cible de stress par établissement.

La capacité réelle dépendra ensuite du modèle de déploiement et du volume de commandes.

---

# 85. Index PostgreSQL

Exemples indispensables :

```sql
CREATE INDEX idx_orders_store_created
ON orders(store_id, created_at DESC);

CREATE INDEX idx_orders_store_status
ON orders(store_id, status);

CREATE INDEX idx_orders_store_payment
ON orders(store_id, payment_status);

CREATE INDEX idx_store_products_store
ON store_products(store_id);

CREATE INDEX idx_memberships_user
ON memberships(user_id);
```

Les index devront être validés avec les plans `EXPLAIN ANALYZE`.

---

# 86. Contraintes métier importantes

### Commande

```text
DINE_IN → table obligatoire
TAKEAWAY → table interdite/null
```

### Paiement

```text
PAID → payment record obligatoire
```

### Fermeture

```text
CLOSED → PAID
```

### Produit

```text
OUT_OF_STOCK → impossible à ajouter
```

### Commande

```text
CANCELLED → aucune transition normale ultérieure
```

---

# 87. Gestion des droits

| Action                | Owner | Admin | Manager | Cashier | Waiter | Kitchen |
| --------------------- | ----: | ----: | ------: | ------: | -----: | ------: |
| Gérer organisation    |     ✅ |     ✅ |       ❌ |       ❌ |      ❌ |       ❌ |
| Gérer catalogue       |     ✅ |     ✅ | partiel |       ❌ |      ❌ |       ❌ |
| Modifier prix central |     ✅ |     ✅ |       ❌ |       ❌ |      ❌ |       ❌ |
| Modifier prix local   |     ✅ |     ✅ |       ✅ |       ❌ |      ❌ |       ❌ |
| Gérer ruptures        |     ✅ |     ✅ |       ✅ |       ❌ |      ❌ |       ❌ |
| Gérer tables          |     ✅ |     ✅ |       ✅ |       ❌ |      ❌ |       ❌ |
| Prendre commande      |     ✅ |     ✅ |       ✅ |       ✅ |      ✅ |       ❌ |
| Voir KDS              |     ✅ |     ✅ |       ✅ |       ❌ |      ❌ |       ❌ |
| Modifier KDS          |     ❌ |     ❌ |       ✅ |       ❌ |      ❌ |       ❌ |
| Livrer                |     ✅ |     ✅ |       ✅ |       ✅ |      ✅ |       ❌ |
| Marquer payé          |     ✅ |     ✅ |       ✅ |       ✅ |      ❌ |       ❌ |

---

# 88. Administration

Dashboard organisation :

```text
ORGANISATION
──────────────────────────────
8 établissements
134 employés
420 produits
```

Liste :

```text
Paris Centre     ACTIVE
Paris Nord       ACTIVE
Lyon             ACTIVE
Marseille        ACTIVE
```

---

# 89. Dashboard établissement

```text
AUJOURD'HUI

Commandes             142
CA déclaré           2 845 €
Non payées              7
En préparation           5
Prêtes                   3
Tables occupées         18
```

---

# 90. Audit fonctionnel

Le manager doit pouvoir retrouver l'historique complet de chaque commande (création, préparation, passage au prêt, livraison, encaissement).

---

# 91. Notifications & Temps Réel

MVP : STOMP / WebSocket (`/store/{storeId}/kitchen`).
V2 : Push Web, SMS, WhatsApp, Email.

---

# 92. Déploiement & CI/CD

Environnement conteneurisé Docker (Traefik, Angular, Spring Boot, PostgreSQL, Redis).
Pipeline CI/CD complet avec tests unitaires, d'intégration, de sécurité, d'E2E et de charge.

---

# 93. Roadmap de développement

* **Sprint 0** : Architecture & Infra (Docker, Spring Boot, Angular, PostgreSQL, Liquibase, Security, RLS)
* **Sprint 1** : Tenant & Identité (Organization, Store, User, Membership, Roles, Auth, RLS)
* **Sprint 2** : Catalogue (Categories, Products, Modifiers, Central/Store Overrides, Availability)
* **Sprint 3** : Tables & Salle (Zones, Tables, Configuration)
* **Sprint 4** : POS (UI Tactile, Panier, Modificateurs, Commandes Dine-in/Takeaway)
* **Sprint 5** : KDS (WebSocket, Queue Cuisine, Écran préparation, Timers)
* **Sprint 6** : Service & Encaissement (Commandes prêtes, Livraison, Paiement déclaratif, Audit, Clôture)
* **Sprint 7** : Hardening (Tests RLS, Tests Sécurité, Tests de Charge Gatling, E2E, Observabilité)
