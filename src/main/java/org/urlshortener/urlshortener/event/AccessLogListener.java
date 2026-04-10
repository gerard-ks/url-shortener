package org.urlshortener.urlshortener.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.urlshortener.urlshortener.entity.AccessLog;
import org.urlshortener.urlshortener.repository.AccessLogRepository;
import org.urlshortener.urlshortener.repository.ShortenedUrlRepository;

@Component
@Slf4j
public class AccessLogListener {

    private final AccessLogRepository accessLogRepository;
    private final ShortenedUrlRepository urlRepository;

    public AccessLogListener(AccessLogRepository accessLogRepository,
                             ShortenedUrlRepository urlRepository) {
        this.accessLogRepository = accessLogRepository;
        this.urlRepository = urlRepository;
    }

    @EventListener
    @Async
    public void onAccessLog(AccessLogEvent event) {
        log.debug("Logging access async for shortCode: {} (thread: {})",
                event.shortCode(), Thread.currentThread());

        try {
            urlRepository.findById(event.urlId()).ifPresent(url -> {
                AccessLog accessLog = AccessLog.recordFor(url);
                accessLogRepository.save(accessLog);
                log.debug("Access logged for shortCode: {}", event.shortCode());
            });
        } catch (Exception e) {
            // Le logging ne doit JAMAIS crasher le système
            log.error("Failed to log access for shortCode: {}", event.shortCode(), e);
        }
    }
}
