package org.urlshortener.urlshortener.exception.security;

import org.urlshortener.urlshortener.exception.base.UrlShortenerException;

public abstract class SecurityException extends UrlShortenerException {

    protected SecurityException(String errorCode, Object... args) {
        super(errorCode, args);
    }
}
