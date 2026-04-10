package org.urlshortener.urlshortener.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.urlshortener.urlshortener.service.interfaces.RedirectService;

@RestController
@Slf4j
@Tag(name = "Redirection", description = "Redirection vers l'URL de destination")
public class RedirectController {

    private final RedirectService redirectService;

    public RedirectController(RedirectService redirectService) {
        this.redirectService = redirectService;
    }

    @Operation(
            summary = "Redirection vers l'URL originale",
            description = """
                    Redirige le visiteur vers l'URL de destination.
                    Incrémente le compteur d'accès.
                    Utilise HTTP 302 (redirection temporaire).
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "302",
                    description = "Redirection vers l'URL de destination",
                    headers = {
                            @Header(name = "Location", description = "URL de destination"),
                            @Header(name = "Cache-Control", description = "no-cache, no-store, must-revalidate"),
                            @Header(name = "X-Short-Code", description = "Code court utilisé")
                    }
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Code court introuvable",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "URL expirée",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/r/{shortCode}")
    public ResponseEntity<Void> redirect(
            @Parameter(description = "Code court de l'URL", example = "abc123")
            @PathVariable String shortCode) {
        log.debug("GET /r/{} - Redirecting", shortCode);

        String destinationUrl = redirectService.redirectAndLog(shortCode);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, destinationUrl)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header("X-Short-Code", shortCode)
                .build();
    }
}
