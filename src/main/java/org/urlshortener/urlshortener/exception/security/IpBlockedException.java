package org.urlshortener.urlshortener.exception.security;

import lombok.Getter;
import org.urlshortener.urlshortener.constants.ErrorCode;

@Getter
public class IpBlockedException extends SecurityException {

    private final int retryAfterSeconds;

    public IpBlockedException(int retryAfterSeconds) {
        super(ErrorCode.IP_BLOCKED, retryAfterSeconds);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
