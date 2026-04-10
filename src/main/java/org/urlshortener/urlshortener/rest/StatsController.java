package org.urlshortener.urlshortener.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.urlshortener.urlshortener.dto.response.StatsResponse;
import org.urlshortener.urlshortener.service.interfaces.StatsService;

@RestController
@RequestMapping("/api/v1/shorten")
@Slf4j
@Tag(name = "Statistiques", description = "Consultation des statistiques d'accès")
public class StatsController {
    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @Operation(
            summary = "Statistiques d'accès",
            description = """
                    Récupère les statistiques d'accès d'une URL.
                    N'incrémente pas le compteur.
                    Accessible sans authentification.
                    Les statistiques d'une URL expirée restent consultables.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Statistiques récupérées",
                    content = @Content(schema = @Schema(implementation = StatsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Code court introuvable",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/{shortCode}/stats")
    public ResponseEntity<StatsResponse> getStats(
            @Parameter(description = "Code court de l'URL", example = "abc123")
            @PathVariable String shortCode) {
        log.info("GET /api/v1/shorten/{}/stats - Getting stats", shortCode);

        StatsResponse response = statsService.getStats(shortCode);

        return ResponseEntity.ok(response);
    }
}
