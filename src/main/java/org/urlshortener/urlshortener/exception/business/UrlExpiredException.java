package org.urlshortener.urlshortener.exception.business;

import lombok.Getter;
import org.urlshortener.urlshortener.constants.ErrorCode;

import java.time.Instant;

@Getter
public class UrlExpiredException extends BusinessRuleException {

    private final String shortCode;
    private final Instant expiredAt;

    public UrlExpiredException(String shortCode, Instant expiredAt) {
        super(ErrorCode.URL_EXPIRED, shortCode);
        this.shortCode = shortCode;
        this.expiredAt = expiredAt;
    }

    public UrlExpiredException(String shortCode) {
        this(shortCode, null);
    }
}
