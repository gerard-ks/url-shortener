package org.urlshortener.urlshortener.exception.business;

import org.urlshortener.urlshortener.exception.base.UrlShortenerException;

public abstract class BusinessRuleException extends UrlShortenerException {

    protected BusinessRuleException(String errorCode, Object... args) {
        super(errorCode, args);
    }
}
