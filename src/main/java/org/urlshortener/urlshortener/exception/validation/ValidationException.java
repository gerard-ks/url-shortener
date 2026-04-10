package org.urlshortener.urlshortener.exception.validation;

import org.urlshortener.urlshortener.exception.base.UrlShortenerException;

public abstract class ValidationException extends UrlShortenerException {

    protected ValidationException(String errorCode, Object... args) {
        super(errorCode, args);
    }

    protected ValidationException(String errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }
}
