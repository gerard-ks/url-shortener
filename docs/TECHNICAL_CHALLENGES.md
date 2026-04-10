# 🧠 Défis techniques & Solutions

---

## 🔴 Défi 1 : Collision de codes courts (Birthday Paradox + Race Condition)

### Problème

Avec un code de 6 caractères Base62, l'espace de clés est de 62⁶ ≈ 56,8 milliards.
Mais le **Birthday Paradox** rend les collisions probables bien avant saturation :

| URLs en base | Probabilité de collision |
|-------------|-------------------------|
| 100 000 | 0.09% |
| 1 000 000 | 9% |
| 10 000 000 | 99% |

De plus, l'approche naïve `generate → existsByShortCode → save` crée une **race condition** :

```
Thread A : generate("abc123") → exists? NO
Thread B : generate("abc123") → exists? NO
Thread A : INSERT "abc123" → ✅ OK
Thread B : INSERT "abc123" → 💥 DUPLICATE KEY
```

### Solution

```
            ❌ AVANT (2 requêtes)              ✅ APRÈS (1 requête)

            generate()                         generate()
                │                                  │
                ▼                                  │
          existsByShortCode()  ← SELECT            │ (supprimé)
                │                                  │
                ▼                                  ▼
             save()            ← INSERT         save()          ← INSERT
                │                                  │
             success                          try / catch
                                                   │
                                      ┌────────────┴────────────┐
                                      │                         │
                                   success              DataIntegrity
                                                       ViolationException
                                                            │
                                                          retry
                                                       (longueur + 1)
```

- **Suppression du `existsByShortCode()`** : 1 requête DB au lieu de 2
- **DB constraint `UNIQUE(short_code)`** comme unique source de vérité
- **`DataIntegrityViolationException`** capturée → retry automatique
- **Longueur progressive** : 6 → 7 → 8 chars après collisions répétées
- **Race condition éliminée** : la base de données garantit l'unicité

---

## 🔴 Défi 2 : Read Path sous forte charge (99% du trafic)

### Problème

L'endpoint de redirection (`GET /r/{code}`) représente 99% du trafic.
Chaque redirection naïve effectue :

```
1 SELECT (trouver l'URL)      → lecture DB
+ 1 INSERT (access_log)        → écriture DB
+ 1 COUNT (stats pour SSE)     → lecture DB
= 3 requêtes DB synchrones par redirection
```

À 5 000 req/s → 15 000 requêtes DB/seconde → saturation.

### Solution

```
┌─────────────────────────────────────────────────────────────┐
│              REDIRECT PATH OPTIMISÉ                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  GET /r/abc123                                               │
│    │                                                         │
│    ├─ @Cacheable (Caffeine in-memory)                        │
│    │  ├─ Cache HIT  → Return URL (< 1ms)   ← 99% des cas  │
│    │  └─ Cache MISS → SELECT DB (< 5ms) → Cache → Return   │
│    │                                                         │
│    ├─ publishEvent(AccessLogEvent)   → async, non-bloquant  │
│    └─ publishEvent(AccessEvent)      → async, non-bloquant  │
│                                                              │
│  Avant : 3 requêtes DB synchrones                           │
│  Après : 0-1 requête DB synchrone + 1 async                │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

- **Spring Cache + Caffeine** : 99% cache hit en régime de croisière
- **@CacheEvict** sur update/delete : cohérence cache automatique
- **Vérification expiration post-cache** : URL expirée détectée même si cachée
- **Migration Redis V2** : changement de config uniquement

---

## 🔴 Défi 3 : Write Path intensif (AccessLog)

### Problème

Chaque clic = 1 INSERT dans `access_logs`.
À 5 000 clics/s → 5 000 INSERT synchrones/seconde dans le chemin critique.

```
❌ Synchrone :
Client → Redirect → INSERT access_log (5-10ms) → 302 Response
                         ↑
                    Bloquant
```

### Solution

```
✅ Asynchrone (Event-Driven) :
Client → Redirect ────────────────────→ 302 Response (< 1ms)
              │
              └─ publishEvent(AccessLogEvent)
                         │
                         ▼ (Virtual Thread, non-bloquant)
                  AccessLogListener
                         │
                         ▼
                  INSERT access_log (en background)
```

- **Spring `ApplicationEventPublisher`** : découplage total
- **`@EventListener @Async`** : INSERT en Virtual Thread séparé
- **Fail-safe** : erreur de logging ne crashe jamais la redirection
- **`AccessEvent` minimal** : contient uniquement `shortCode`

---

## 🔴 Défi 4 : Timing Attack sur l'authentification

### Problème

La comparaison naïve de hash révèle des informations par timing :

```
❌ String.equals() :
"aaa..." vs "zzz..." → false immédiatement (1er char différent)
"e5b..." vs "e5b..." → compare tous les chars (plus lent)
→ L'attaquant déduit le hash caractère par caractère
```

### Solution

```java
// Comparaison en temps CONSTANT
private boolean timingSafeEquals(String a, String b) {
    if (a.length() != b.length()) return false;
    int result = 0;
    for (int i = 0; i < a.length(); i++) {
        result |= a.charAt(i) ^ b.charAt(i);
    }
    return result == 0;
}
```

De plus, séparation `findByShortCode()` + `verifyToken()` :

```
URL inexistante → 404 (correct)
Token invalide  → 403 (correct)
```

---

## 🔴 Défi 5 : SSE scalable

### Problème

1 000 clients SSE = 1 000 connexions ouvertes.
ThreadPool classique (100 threads) → maximum 100 clients.

### Solution

```
Virtual Threads      : 1 000 000+ threads possibles
ConcurrentHashMap    : thread-safe, lock par segment
CopyOnWriteArraySet  : optimisé lecture (broadcast) vs écriture (subscribe)
Interface SseEventBus: migration Redis PubSub en V2
Cleanup automatique  : onCompletion, onTimeout, onError → 0 fuite mémoire
```

---

## 🔴 Défi 6 : Suppression massive sans lock de table

### Problème

```
❌ DELETE FROM shortened_urls WHERE expires_at < threshold;
→ Lock table 30+ secondes → toutes les redirections bloquées
```

### Solution

```
✅ Batch delete :
SELECT 1000 IDs → DELETE → COMMIT → (traffic normal) → repeat
```

- **Task + Service séparés** : évite `@Transactional` self-invocation
- **Chaque batch = 1 petite transaction**
- **`@CacheEvict(allEntries = true)`** après nettoyage

---

## 🔴 Défi 7 : Validation DRY

### Problème

Règles dupliquées entre DTO, Service et Entité.

### Solution

```
DTO    → Format syntaxique (@NotBlank, @Size, @Pattern)
Service → Règles applicatives (auto-référence, unicité)
Entité  → Invariants domaine (codes réservés, expiration max)

Chaque règle existe à UN SEUL endroit.
```

---