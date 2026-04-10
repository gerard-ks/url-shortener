package org.urlshortener.urlshortener.mapper;

import org.urlshortener.urlshortener.dto.response.CreateUrlResponse;
import org.urlshortener.urlshortener.dto.response.StatsResponse;
import org.urlshortener.urlshortener.dto.response.UrlMetadataResponse;
import org.urlshortener.urlshortener.entity.ShortenedUrl;
import org.urlshortener.urlshortener.repository.UrlWithStatsProjection;

public final class ShortenMapper {
    private ShortenMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static CreateUrlResponse toCreateResponse(ShortenedUrl url, String token, String baseUrl) {
        return new CreateUrlResponse(
                url.getId().toString(),
                url.getUrl(),
                url.getShortCode(),
                buildShortUrl(baseUrl, url.getShortCode()),
                token,
                url.getCreatedAt(),
                url.getUpdatedAt(),
                url.getExpiresAt()
        );
    }

    public static UrlMetadataResponse toMetadataResponse(ShortenedUrl url, String baseUrl) {
        return new UrlMetadataResponse(
                url.getId().toString(),
                url.getUrl(),
                url.getShortCode(),
                buildShortUrl(baseUrl, url.getShortCode()),
                url.getCreatedAt(),
                url.getUpdatedAt(),
                url.getExpiresAt()
        );
    }

    public static StatsResponse toStatsResponse(UrlWithStatsProjection projection, String baseUrl) {
        boolean expired = projection.getExpiresAt() != null
                && projection.getExpiresAt().isBefore(java.time.Instant.now());

        return new StatsResponse(
                projection.getId().toString(),
                projection.getUrl(),
                projection.getShortCode(),
                buildShortUrl(baseUrl, projection.getShortCode()),
                projection.getCreatedAt(),
                projection.getUpdatedAt(),
                projection.getExpiresAt(),
                expired,
                new StatsResponse.Stats(
                        projection.getAccessCount() != null ? projection.getAccessCount() : 0,
                        projection.getLastAccessedAt()
                )
        );
    }

    private static String buildShortUrl(String baseUrl, String shortCode) {
        return baseUrl + "/r/" + shortCode;
    }
}
