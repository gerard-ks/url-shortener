package org.urlshortener.urlshortener.exception.validation;

import org.urlshortener.urlshortener.constants.ErrorCode;

public class InvalidShortCodeException extends ValidationException {
    public InvalidShortCodeException(String errorCode, Object... args) {
        super(errorCode, args);
    }

    public static InvalidShortCodeException empty() {
        return new InvalidShortCodeException(ErrorCode.SHORT_CODE_EMPTY);
    }

    public static InvalidShortCodeException tooShort(int minLength) {
        return new InvalidShortCodeException(ErrorCode.SHORT_CODE_TOO_SHORT, minLength);
    }

    public static InvalidShortCodeException tooLong(int maxLength) {
        return new InvalidShortCodeException(ErrorCode.SHORT_CODE_TOO_LONG, maxLength);
    }

    public static InvalidShortCodeException invalidCharacters() {
        return new InvalidShortCodeException(ErrorCode.SHORT_CODE_INVALID_CHARACTERS);
    }

    public static InvalidShortCodeException reserved(String shortCode) {
        return new InvalidShortCodeException(ErrorCode.SHORT_CODE_RESERVED, shortCode);
    }
}
