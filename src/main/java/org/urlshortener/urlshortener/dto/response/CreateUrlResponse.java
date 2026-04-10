package org.urlshortener.urlshortener.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record CreateUrlResponse(
        String id,
        String url,

        @JsonProperty("short_code")
        String shortCode,

        @JsonProperty("short_url")
        String shortUrl,

        /**
         * Token de propriété (retourné UNE SEULE FOIS).
         * L'utilisateur DOIT le sauvegarder.
         */
        String token,

        @JsonProperty("created_at")
        Instant createdAt,

        @JsonProperty("updated_at")
        Instant updatedAt,

        @JsonProperty("expires_at")
        Instant expiresAt
) {
}
