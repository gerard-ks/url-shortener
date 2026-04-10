package org.urlshortener.urlshortener.service.interfaces;

import java.time.Instant;

public interface UrlCleanupService {
    int deleteExpiredBatch(Instant threshold);
    long countExpired(Instant threshold);
}
