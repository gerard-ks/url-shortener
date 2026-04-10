package org.urlshortener.urlshortener.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record UrlMetadataResponse(
        String id,
        String url,

        @JsonProperty("short_code")
        String shortCode,

        @JsonProperty("short_url")
        String shortUrl,

        @JsonProperty("created_at")
        Instant createdAt,

        @JsonProperty("updated_at")
        Instant updatedAt,

        @JsonProperty("expires_at")
        Instant expiresAt
) {}
