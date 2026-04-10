package org.urlshortener.urlshortener.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.urlshortener.urlshortener.dto.request.CreateUrlRequest;
import org.urlshortener.urlshortener.dto.request.UpdateUrlRequest;
import org.urlshortener.urlshortener.dto.response.CreateUrlResponse;
import org.urlshortener.urlshortener.dto.response.UrlMetadataResponse;
import org.urlshortener.urlshortener.exception.business.NoFieldToUpdateException;
import org.urlshortener.urlshortener.filter.security.TokenAuthenticationFilter;
import org.urlshortener.urlshortener.service.interfaces.ShortenService;

@RestController
@RequestMapping("/api/v1/shorten")
@Slf4j
@Tag(name = "URL Shortener", description = "Gestion des URLs raccourcies (CRUD)")
public class ShortenController {

    private final ShortenService shortenService;

    public ShortenController(ShortenService shortenService) {
        this.shortenService = shortenService;
    }

    @Operation(
            summary = "Créer une URL courte",
            description = """
                    Transforme une URL longue en code court unique.
                    Retourne le token de propriété (à sauvegarder, affiché une seule fois).
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "URL créée avec succès",
                    content = @Content(schema = @Schema(implementation = CreateUrlResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Données invalides (URL, code court, expiration)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Code court déjà utilisé",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Trop de requêtes",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @PostMapping
    public ResponseEntity<CreateUrlResponse> create(@Valid @RequestBody CreateUrlRequest request) {
        log.info("POST /api/v1/shorten - Creating short URL for: {}", request.url());

        CreateUrlResponse response = shortenService.create(
                request.url(),
                request.customCode(),
                request.expiresAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Récupérer les métadonnées",
            description = "Consulte les informations d'une URL courte sans déclencher de redirection ni incrémenter le compteur."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Métadonnées récupérées",
                    content = @Content(schema = @Schema(implementation = UrlMetadataResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Code court introuvable",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/{shortCode}")
    public ResponseEntity<UrlMetadataResponse> getMetadata(
            @Parameter(description = "Code court de l'URL", example = "abc123")
            @PathVariable String shortCode) {
        log.info("GET /api/v1/shorten/{} - Getting metadata", shortCode);

        UrlMetadataResponse response = shortenService.getMetadata(shortCode);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Modifier une URL",
            description = """
                    Modifie l'URL de destination et/ou la date d'expiration.
                    Nécessite le token de propriété.
                    Au moins un champ doit être fourni.
                    """,
            security = @SecurityRequirement(name = "Bearer Token")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "URL modifiée avec succès",
                    content = @Content(schema = @Schema(implementation = UrlMetadataResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Données invalides ou aucun champ fourni",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token manquant",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Token invalide",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Code court introuvable",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "URL expirée, modification impossible",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @PutMapping("/{shortCode}")
    public ResponseEntity<UrlMetadataResponse> update(
            @Parameter(description = "Code court de l'URL", example = "abc123")
            @PathVariable String shortCode,
            @Valid @RequestBody UpdateUrlRequest request,
            HttpServletRequest httpRequest) {

        log.info("PUT /api/v1/shorten/{} - Updating URL", shortCode);

        String token = getToken(httpRequest);

        if (!request.hasAtLeastOneField()) {
            throw new NoFieldToUpdateException();
        }

        UrlMetadataResponse response = shortenService.update(
                shortCode,
                token,
                request.url(),
                request.expiresAt()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Supprimer une URL",
            description = """
                    Supprime définitivement une URL raccourcie et tous ses logs d'accès.
                    Nécessite le token de propriété.
                    Le code court redevient disponible après suppression.
                    """,
            security = @SecurityRequirement(name = "Bearer Token")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "URL supprimée avec succès"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token manquant",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Token invalide",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Code court introuvable",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Code court de l'URL", example = "abc123")
            @PathVariable String shortCode,
            HttpServletRequest httpRequest) {

        log.info("DELETE /api/v1/shorten/{} - Deleting URL", shortCode);

        String token = getToken(httpRequest);

        shortenService.delete(shortCode, token);

        return ResponseEntity.noContent().build();
    }


    private String getToken(HttpServletRequest request) {
        return (String) request.getAttribute(TokenAuthenticationFilter.TOKEN_ATTRIBUTE);
    }
}
