package org.urlshortener.urlshortener.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.urlshortener.urlshortener.repository.AccessLogRepository;
import org.urlshortener.urlshortener.repository.ShortenedUrlRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
public class InMemorySseEventBus implements SseEventBus {

    private final Map<String, Set<SseEmitter>> subscriptions = new ConcurrentHashMap<>();
    private final AccessLogRepository accessLogRepository;
    private final ShortenedUrlRepository urlRepository;

    public InMemorySseEventBus(AccessLogRepository accessLogRepository, ShortenedUrlRepository urlRepository) {
        this.accessLogRepository = accessLogRepository;
        this.urlRepository = urlRepository;
    }

    @Override
    public void subscribe(String shortCode, SseEmitter emitter) {
        subscriptions.computeIfAbsent(shortCode, k -> new CopyOnWriteArraySet<>())
                .add(emitter);

        log.info("Client subscribed to SSE for shortCode: {}", shortCode);

        // Cleanup automatique à la déconnexion
        emitter.onCompletion(() -> unsubscribe(shortCode, emitter));
        emitter.onTimeout(() -> unsubscribe(shortCode, emitter));
        emitter.onError((e) -> unsubscribe(shortCode, emitter));
    }


    @Override
    public void unsubscribe(String shortCode, SseEmitter emitter) {
        Set<SseEmitter> emitters = subscriptions.get(shortCode);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                subscriptions.remove(shortCode);
            }
        }
        log.info("Client unsubscribed from SSE for shortCode: {}", shortCode);
    }


    @EventListener
    @Async
    @Override
    public void onAccessEvent(AccessEvent event) {
        Set<SseEmitter> emitters = subscriptions.get(event.shortCode());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        log.debug("Broadcasting access event to {} clients for shortCode: {}",
                emitters.size(), event.shortCode());

        // Stats calculées ICI (consumer), pas dans le redirect path
        long accessCount = urlRepository.findByShortCode(event.shortCode())
                .map(url -> accessLogRepository.countByShortenedUrlId(url.getId()))
                .orElse(0L);

        Instant lastAccessedAt = Instant.now();

        emitters.forEach(emitter -> sendEvent(emitter, event.shortCode(), accessCount, lastAccessedAt));
    }


    public int getSubscriberCount(String shortCode) {
        Set<SseEmitter> emitters = subscriptions.get(shortCode);
        return emitters != null ? emitters.size() : 0;
    }

    private void sendEvent(SseEmitter emitter, String shortCode,
                           long accessCount, Instant lastAccessedAt) {
        try {
            emitter.send(SseEmitter.event()
                    .name("access")
                    .data(Map.of(
                            "shortCode", shortCode,
                            "accessCount", accessCount,
                            "lastAccessedAt", lastAccessedAt.toString()
                    ))
            );
        } catch (IOException e) {
            log.warn("Failed to send SSE, removing dead emitter", e);
            unsubscribe(shortCode, emitter);
            emitter.completeWithError(e);
        }
    }
}
