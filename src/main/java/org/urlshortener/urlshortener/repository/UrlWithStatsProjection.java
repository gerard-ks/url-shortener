package org.urlshortener.urlshortener.repository;

import java.time.Instant;
import java.util.UUID;

public interface UrlWithStatsProjection {
    // Champs de ShortenedUrl
    UUID getId();
    String getShortCode();
    String getUrl();
    Instant getCreatedAt();
    Instant getUpdatedAt();
    Instant getExpiresAt();

    // Champs calculés (stats)
    Long getAccessCount();
    Instant getLastAccessedAt();
}
