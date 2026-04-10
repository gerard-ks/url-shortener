package org.urlshortener.urlshortener.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Requête de création d'une URL raccourcie")
public record CreateUrlRequest(
        @Schema(
                description = "URL de destination à raccourcir",
                example = "https://www.example.com/very/long/path?param=value",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "{URL_EMPTY}")
        @Size(max = 2048, message = "{URL_TOO_LONG}")
        @Pattern(
                regexp = "^https?://.*",
                message = "{URL_INVALID_PROTOCOL}"
        )
        String url,

        @Schema(
                description = "Code court personnalisé (optionnel, 4-20 caractères)",
                example = "promo2025",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("custom_code")
        @Size(min = 4, max = 20, message = "{SHORT_CODE_TOO_SHORT}")
        @Pattern(
                regexp = "^[a-zA-Z0-9_-]+$",
                message = "{SHORT_CODE_INVALID_CHARACTERS}"
        )
        String customCode,

        @Schema(
                description = "Date d'expiration ISO 8601 (optionnel, défaut: +365 jours)",
                example = "2025-12-31T23:59:59Z",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("expires_at")
        @Future(message = "{EXPIRATION_IN_PAST}")
        Instant expiresAt
) {
    public CreateUrlRequest {
        // Normalisation : trim si customCode fourni
        if (customCode != null) {
            customCode = customCode.trim();
            if (customCode.isEmpty()) {
                customCode = null;
            }
        }
    }
}
