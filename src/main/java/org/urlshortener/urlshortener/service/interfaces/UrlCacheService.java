package org.urlshortener.urlshortener.service.interfaces;

import org.urlshortener.urlshortener.entity.ShortenedUrl;

public interface UrlCacheService {
    ShortenedUrl findActiveUrl(String shortCode);
}
