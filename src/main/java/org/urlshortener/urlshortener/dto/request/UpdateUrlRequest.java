package org.urlshortener.urlshortener.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Requête de modification d'une URL raccourcie (au moins un champ requis)")
public record UpdateUrlRequest(
        @Schema(
                description = "Nouvelle URL de destination",
                example = "https://www.example.com/new-destination",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 2048, message = "URL cannot exceed 2048 characters")
        @Pattern(
                regexp = "^https?://.*",
                message = "URL must start with http:// or https://"
        )
        String url,

        @Schema(
                description = "Nouvelle date d'expiration (null = permanent)",
                example = "2026-06-30T23:59:59Z",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("expires_at")
        @Future(message = "Expiration date must be in the future")
        Instant expiresAt
) {
    public boolean hasAtLeastOneField() {
        return url != null || expiresAt != null;
    }
}
