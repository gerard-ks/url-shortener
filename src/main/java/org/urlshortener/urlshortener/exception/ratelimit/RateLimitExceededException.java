package org.urlshortener.urlshortener.exception.ratelimit;

import lombok.Getter;
import org.urlshortener.urlshortener.constants.ErrorCode;
import org.urlshortener.urlshortener.exception.base.UrlShortenerException;

@Getter
public class RateLimitExceededException extends UrlShortenerException {

    private final int retryAfterSeconds;

    public RateLimitExceededException(int retryAfterSeconds) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED, retryAfterSeconds);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
