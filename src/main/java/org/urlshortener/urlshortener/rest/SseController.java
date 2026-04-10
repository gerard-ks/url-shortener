package org.urlshortener.urlshortener.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.urlshortener.urlshortener.dto.response.StatsResponse;
import org.urlshortener.urlshortener.event.SseEventBus;
import org.urlshortener.urlshortener.service.interfaces.StatsService;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/shorten")
@Slf4j
@Tag(name = "Temps réel (SSE)", description = "Statistiques en temps réel via Server-Sent Events")
public class SseController {

    @Value("${app.sse.timeout:300000}")
    private long sseTimeout;

    private final SseEventBus eventBus;
    private final StatsService statsService;

    public SseController(SseEventBus eventBus, StatsService statsService) {
        this.eventBus = eventBus;
        this.statsService = statsService;
    }

    @Operation(
            summary = "Flux temps réel (SSE)",
            description = """
                    Ouvre une connexion Server-Sent Events pour recevoir
                    les statistiques en temps réel.
                    
                    Événements :
                    - `initial` : statistiques actuelles (envoyé immédiatement)
                    - `access` : mise à jour à chaque accès
                    ```
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Connexion SSE établie (flux continu)",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Code court introuvable",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping(value = "/{shortCode}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @Parameter(description = "Code court de l'URL", example = "abc123")
            @PathVariable String shortCode) {
        log.info("GET /api/v1/shorten/{}/events - New SSE subscription", shortCode);

        SseEmitter emitter = new SseEmitter(sseTimeout);

        // Envoyer l'état initial
        sendInitialStats(emitter, shortCode);

        // Abonner aux événements futurs
        eventBus.subscribe(shortCode, emitter);

        return emitter;
    }

    private void sendInitialStats(SseEmitter emitter, String shortCode) {
        try {
            StatsResponse stats = statsService.getStats(shortCode);

            emitter.send(SseEmitter.event()
                    .name("initial")
                    .data(stats)
            );

            log.debug("Initial stats sent for shortCode: {}", shortCode);
        } catch (IOException e) {
            log.error("Failed to send initial stats for shortCode: {}", shortCode, e);
            emitter.completeWithError(e);
        }
    }
}

