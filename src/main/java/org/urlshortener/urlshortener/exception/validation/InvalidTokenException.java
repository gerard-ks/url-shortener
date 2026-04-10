package org.urlshortener.urlshortener.exception.validation;

import org.urlshortener.urlshortener.constants.ErrorCode;

public class InvalidTokenException extends ValidationException {

    private InvalidTokenException(String errorCode, Object... args) {
        super(errorCode, args);
    }

    public static InvalidTokenException empty() {
        return new InvalidTokenException(ErrorCode.TOKEN_EMPTY);
    }

    public static InvalidTokenException invalidFormat() {
        return new InvalidTokenException(ErrorCode.TOKEN_INVALID_FORMAT);
    }
}
