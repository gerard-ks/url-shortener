package org.urlshortener.urlshortener.exception.resource;

import org.urlshortener.urlshortener.constants.ErrorCode;

public class UrlNotFoundException extends ResourceNotFoundException {

    public UrlNotFoundException(String shortCode) {
        super(ErrorCode.URL_NOT_FOUND, shortCode);
    }
}
