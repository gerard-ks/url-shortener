# 🔗 URL Shortener — Service de Raccourcissement d'URL

> **Transformer des URLs longues et complexes en liens courts, traçables et gérables.**

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

```
❌ https://www.example.com/products/category/electronics/smartphones?color=blue&utm_source=newsletter&utm_campaign=winter2025
✅ https://short.url/r/iP15pro
```

---

## ✨ Fonctionnalités

- 🔗 **Raccourcissement d'URL** — Code court 6 caractères Base62
- ✏️ **Code personnalisé** — Choisir son propre code (`/r/promo2025`)
- 🔄 **Modification post-partage** — Changer la destination sans changer le lien
- ⏰ **Expiration automatique** — Liens qui expirent (410 Gone)
- 📊 **Statistiques temps réel** — Server-Sent Events (SSE)
- 🔒 **Token de propriété** — Sécurité sans compte utilisateur
- 🛡️ **Rate limiting** — Protection par IP et par endpoint
- 🌍 **Internationalisation** — EN/FR

---

## 🧠 Défis techniques résolus

Ce projet adresse **7 défis d'ingénierie majeurs**. Détails complets → [docs/TECHNICAL_CHALLENGES.md](docs/TECHNICAL_CHALLENGES.md)

| Défi | Problème | Solution |
|------|----------|----------|
| **Collision de codes** | Birthday Paradox + race condition | DB constraint + retry (0 SELECT, 1 INSERT) |
| **Read path** | 3 requêtes DB synchrones par redirection | Cache Caffeine + 0 write synchrone |
| **Write path** | 5 000 INSERT/s dans le chemin critique | Event-driven async (Virtual Threads) |
| **Timing attack** | Comparaison de hash révèle des informations | `timingSafeEquals()` + séparation 404/403 |
| **SSE scalable** | 1 000+ connexions persistantes simultanées | Virtual Threads + ConcurrentHashMap |
| **Suppression massive** | Lock table pendant les DELETE | Batch delete (petites transactions) |
| **Validation DRY** | Règles dupliquées DTO/Service/Entité | 3 couches sans duplication |

---

## 🏗 Architecture

Détails complets → [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

```
Client → Filters (RateLimit, TokenAuth)
       → Controllers (Shorten, Redirect, Stats, SSE)
       → Services (ShortenService, RedirectService, TokenService...)
       → Repository (Spring Data JPA) + Cache (Caffeine)
       → PostgreSQL
       
       + Event System (AccessEvent → AsyncListener → SSE broadcast)
       + Scheduled Tasks (Cleanup batch URLs expirées)
```

### Décisions clés

| Décision | Choix | Pourquoi pas l'alternative |
|----------|-------|---------------------------|
| Cache | Caffeine (in-memory) | Redis = infra supplémentaire, migration transparente en V2 |
| Async | Virtual Threads (Java 21) | ThreadPool = pool sizing manuel, scaling limité |
| Auth | Token simple (`tok_xxx`) | JWT = over-engineering sans comptes utilisateurs |
| SSE | Interface + InMemory | WebSocket = bidirectionnel inutile ici |
| Collisions | DB constraint + retry | `existsByShortCode()` = 2 requêtes + race condition |
| Erreurs | ProblemDetail (RFC 7807) | ErrorResponse custom = non standard |

---

## 🛠 Stack technique

| Composant | Technologie |
|-----------|-------------|
| **Runtime** | Java 21 (Virtual Threads) |
| **Framework** | Spring Boot 3.2+ |
| **Base de données** | PostgreSQL 15+ |
| **Cache** | Caffeine (in-memory) |
| **Rate Limiting** | Bucket4j |
| **Migrations** | Flyway |
| **Documentation** | SpringDoc OpenAPI 3 |
| **i18n** | Spring MessageSource (EN/FR) |

---

## 🚀 Démarrage rapide

### Prérequis

- Java 21+
- Maven 3.9+
- PostgreSQL 15+

### Lancer

```bash
# 1. Cloner
git clone https://github.com/gerard-ks/url-shortener.git
cd url-shortener

# 2. Base de données
psql -U postgres -c "CREATE DATABASE urlshortener;"

# 3. Configuration
cp .env.example .env
# Modifier : DATABASE_PASSWORD, TOKEN_SECRET

# 4. Lancer
mvn spring-boot:run

# 5. Vérifier
open http://localhost:8080/swagger-ui.html
```

### Docker

```bash
cp .env.example .env
docker compose up -d
open http://localhost:8080/swagger-ui.html
```

---
