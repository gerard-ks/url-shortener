package org.urlshortener.urlshortener.filter.ratelimit;

public interface RateLimitService {
    RateLimitResult tryConsume(String ip, String method, String path);
}
