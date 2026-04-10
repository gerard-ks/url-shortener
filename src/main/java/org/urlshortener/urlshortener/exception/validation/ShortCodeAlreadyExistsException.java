package org.urlshortener.urlshortener.exception.validation;

import org.urlshortener.urlshortener.constants.ErrorCode;

public class ShortCodeAlreadyExistsException extends ValidationException{
    public ShortCodeAlreadyExistsException(String shortCode) {
        super(ErrorCode.SHORT_CODE_ALREADY_EXISTS, shortCode);
    }
}
