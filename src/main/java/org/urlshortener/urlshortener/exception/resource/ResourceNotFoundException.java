package org.urlshortener.urlshortener.exception.resource;

import org.urlshortener.urlshortener.exception.base.UrlShortenerException;

public abstract class ResourceNotFoundException extends UrlShortenerException {

    protected ResourceNotFoundException(String errorCode, Object... args) {
        super(errorCode, args);
    }
}
