package org.urlshortener.urlshortener.exception.base;

import lombok.Getter;

import java.util.Arrays;

@Getter
public abstract class UrlShortenerException extends RuntimeException {
    private final String errorCode;
    private final transient Object[] args;

    protected UrlShortenerException(String errorCode, Object... args) {
        super(errorCode);
        this.errorCode = errorCode;
        this.args = args != null ? Arrays.copyOf(args, args.length) : new Object[0];
    }

    protected UrlShortenerException(String errorCode, Throwable cause, Object... args) {
        super(errorCode, cause);
        this.errorCode = errorCode;
        this.args = args != null ? Arrays.copyOf(args, args.length) : new Object[0];
    }
}
