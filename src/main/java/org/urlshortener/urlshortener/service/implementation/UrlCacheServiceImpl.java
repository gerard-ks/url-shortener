package org.urlshortener.urlshortener.service.implementation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.urlshortener.urlshortener.entity.ShortenedUrl;
import org.urlshortener.urlshortener.exception.business.CannotAccessExpiredUrlException;
import org.urlshortener.urlshortener.exception.resource.UrlNotFoundException;
import org.urlshortener.urlshortener.repository.ShortenedUrlRepository;
import org.urlshortener.urlshortener.service.interfaces.UrlCacheService;

@Service
@Slf4j
@Transactional(readOnly = true)
public class UrlCacheServiceImpl implements UrlCacheService {
    private final ShortenedUrlRepository urlRepository;

    public UrlCacheServiceImpl(ShortenedUrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    @Cacheable(value = "urls", key = "#shortCode")
    @Override
    public ShortenedUrl findActiveUrl(String shortCode) {
        log.debug("Cache miss for shortCode: {}", shortCode);

        ShortenedUrl url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (url.isExpired()) {
            throw new CannotAccessExpiredUrlException(shortCode);
        }

        return url;
    }
}
