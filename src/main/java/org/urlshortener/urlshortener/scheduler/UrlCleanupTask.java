package org.urlshortener.urlshortener.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.urlshortener.urlshortener.config.CleanupProperties;
import org.urlshortener.urlshortener.service.interfaces.UrlCleanupService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
public class UrlCleanupTask {

    private final UrlCleanupService cleanupService;
    private final CleanupProperties cleanupProperties;

    public UrlCleanupTask(UrlCleanupService cleanupService, CleanupProperties cleanupProperties) {
        this.cleanupService = cleanupService;
        this.cleanupProperties = cleanupProperties;
    }


    @Scheduled(cron = "${app.cleanup.cron:0 0 3 * * *}")
    @CacheEvict(value = "urls", allEntries = true)
    public void cleanExpiredUrls() {
        if (!cleanupProperties.isEnabled()) {
            log.info("Cleanup task disabled, skipping");
            return;
        }

        log.info("Starting expired URLs cleanup task");

        Instant threshold = Instant.now().minus(
                cleanupProperties.getRetentionDays(), ChronoUnit.DAYS
        );

        long totalExpired = cleanupService.countExpired(threshold);

        if (totalExpired == 0) {
            log.info("No expired URLs to clean up");
            return;
        }

        log.info("Found {} expired URLs older than {} days",
                totalExpired, cleanupProperties.getRetentionDays());

        // Suppression par batch (chaque batch = 1 transaction)
        int totalDeleted = 0;
        int batchDeleted;

        do {
            batchDeleted = cleanupService.deleteExpiredBatch(threshold);
            totalDeleted += batchDeleted;
        } while (batchDeleted > 0);

        log.info("Cleanup completed: {} URLs deleted", totalDeleted);
    }
}
