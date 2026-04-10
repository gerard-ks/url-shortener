package org.urlshortener.urlshortener.service.interfaces;

import org.urlshortener.urlshortener.dto.response.CreateUrlResponse;
import org.urlshortener.urlshortener.dto.response.UrlMetadataResponse;

import java.time.Instant;

public interface ShortenService {
    CreateUrlResponse create(String url, String customCode, Instant expiresAt);
    UrlMetadataResponse getMetadata(String shortCode);
    UrlMetadataResponse update(String shortCode, String token,
                               String newUrl, Instant newExpiresAt);
    void delete(String shortCode, String token);
}
