package org.urlshortener.urlshortener.service.implementation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.urlshortener.urlshortener.config.CleanupProperties;
import org.urlshortener.urlshortener.repository.ShortenedUrlRepository;
import org.urlshortener.urlshortener.service.interfaces.UrlCleanupService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class UrlCleanupServiceImpl implements UrlCleanupService {

    private final ShortenedUrlRepository repository;
    private final CleanupProperties cleanupProperties;

    public UrlCleanupServiceImpl(ShortenedUrlRepository repository, CleanupProperties cleanupProperties) {
        this.repository = repository;
        this.cleanupProperties = cleanupProperties;
    }

    @Transactional
    @Override
    public int deleteExpiredBatch(Instant threshold) {
        int batchSize = cleanupProperties.getBatchSize();

        List<UUID> expiredIds = repository.findExpiredIdsBefore(threshold, batchSize);

        if (expiredIds.isEmpty()) {
            return 0;
        }

        int deleted = repository.deleteByIds(expiredIds);

        log.debug("Deleted batch of {} expired URLs", deleted);

        return deleted;
    }


    @Transactional(readOnly = true)
    @Override
    public long countExpired(Instant threshold) {
        return repository.countExpiredBefore(threshold);
    }
}
