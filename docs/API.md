# 📖 Documentation API

---

## Swagger UI

```
http://localhost:8080/swagger-ui.html
```

---

## Exemples détaillés

### Créer une URL courte

```bash
curl -X POST http://localhost:8080/api/v1/shorten \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.example.com/very/long/path",
    "custom_code": "promo2025",
    "expires_at": "2025-12-31T23:59:59Z"
  }'
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "url": "https://www.example.com/very/long/path",
  "short_code": "promo2025",
  "short_url": "http://localhost:8080/r/promo2025",
  "token": "tok_aB3xY9kLmN2pQ5rT8vW1zC4fH7jK0sD",
  "created_at": "2025-01-20T10:30:00Z",
  "expires_at": "2025-12-31T23:59:59Z"
}
```

> ⚠️ **Sauvegardez le `token` !** Il ne sera plus jamais affiché.

---

### Redirection

```bash
curl -L http://localhost:8080/r/promo2025
# → 302 Found → https://www.example.com/very/long/path
```

---

### Consulter les métadonnées

```bash
curl http://localhost:8080/api/v1/shorten/promo2025
```

---

### Modifier (avec token)

```bash
curl -X PUT http://localhost:8080/api/v1/shorten/promo2025 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer tok_aB3xY9kLmN2pQ5rT8vW1zC4fH7jK0sD" \
  -d '{"url": "https://www.example.com/new-destination"}'
```

---

### Supprimer

```bash
curl -X DELETE http://localhost:8080/api/v1/shorten/promo2025 \
  -H "Authorization: Bearer tok_aB3xY9kLmN2pQ5rT8vW1zC4fH7jK0sD"
# → 204 No Content
```

---

### Statistiques

```bash
curl http://localhost:8080/api/v1/shorten/promo2025/stats
```

```json
{
  "short_code": "promo2025",
  "expired": false,
  "stats": {
    "access_count": 142,
    "last_accessed_at": "2025-01-20T15:30:00Z"
  }
}
```

---

### Statistiques temps réel (SSE)

```javascript
const es = new EventSource('/api/v1/shorten/promo2025/events');

es.addEventListener('initial', (e) => {
    console.log('Stats initiales:', JSON.parse(e.data));
});

es.addEventListener('access', (e) => {
    const data = JSON.parse(e.data);
    console.log(`Nouvel accès ! Total: ${data.accessCount}`);
});
```

---

## Format des erreurs (RFC 7807)

```bash
curl -H "Accept-Language: fr" http://localhost:8080/api/v1/shorten/unknown
```

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Aucune URL trouvée pour le code court 'unknown'",
  "code": "URL_NOT_FOUND",
  "timestamp": "2025-01-20T15:30:00Z"
}
```

### Codes d'erreur

| HTTP | Code | Description |
|------|------|-------------|
| 400 | URL_EMPTY, URL_TOO_LONG, URL_INVALID_PROTOCOL | Validation URL |
| 400 | SHORT_CODE_TOO_SHORT, SHORT_CODE_RESERVED | Validation code |
| 400 | NO_FIELD_TO_UPDATE | Aucun champ fourni |
| 401 | TOKEN_MISSING | Header Authorization absent |
| 403 | TOKEN_MISMATCH | Token invalide |
| 404 | URL_NOT_FOUND | Code court inexistant |
| 409 | SHORT_CODE_ALREADY_EXISTS | Code déjà pris |
| 410 | URL_EXPIRED | URL expirée |
| 429 | RATE_LIMIT_EXCEEDED | Trop de requêtes |
| 500 | INTERNAL_ERROR | Erreur serveur |

### Rate Limiting Headers

```http
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 7
X-RateLimit-Reset: 1705312800
Retry-After: 60
```

---