package org.urlshortener.urlshortener.filter.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.urlshortener.urlshortener.config.RateLimitProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class InMemoryRateLimitService implements RateLimitService {

    private final RateLimitProperties rateLimitProperties;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult tryConsume(String ip, String method, String path) {
        if (!rateLimitProperties.isEnabled()) {
            return RateLimitResult.success();
        }

        RateLimitProperties.EndpointLimit limit = rateLimitProperties.getLimitForEndpoint(method, path);
        String bucketKey = buildBucketKey(ip, method, path);

        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(limit));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return RateLimitResult.success(
                    limit.getMaxRequests(),
                    probe.getRemainingTokens(),
                    calculateResetTime(limit.getWindowSeconds())
            );
        } else {
            log.warn("Rate limit exceeded for IP {} on {} {}", ip, method, path);
            return RateLimitResult.exceeded(
                    limit.getMaxRequests(),
                    0,
                    calculateResetTime(limit.getWindowSeconds()),
                    limit.getWindowSeconds()
            );
        }
    }

    private String buildBucketKey(String ip, String method, String path) {
        return String.format("%s:%s:%s", ip, method, path);
    }

    private Bucket createBucket(RateLimitProperties.EndpointLimit limit) {
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(limit.getMaxRequests())
                .refillIntervally(limit.getMaxRequests(), Duration.ofSeconds(limit.getWindowSeconds()))
                .build();
        return Bucket.builder().addLimit(bandwidth).build();
    }

    private long calculateResetTime(int windowSeconds) {
        return Instant.now().plusSeconds(windowSeconds).getEpochSecond();
    }
}
