package org.urlshortener.urlshortener.exception.validation;

import org.urlshortener.urlshortener.constants.ErrorCode;

public class InvalidUrlException extends ValidationException {
    private InvalidUrlException(String errorCode, Object... args) {
        super(errorCode, args);
    }

    public static InvalidUrlException empty() {
        return new InvalidUrlException(ErrorCode.URL_EMPTY);
    }

    public static InvalidUrlException tooLong(int maxLength) {
        return new InvalidUrlException(ErrorCode.URL_TOO_LONG, maxLength);
    }

    public static InvalidUrlException invalidProtocol() {
        return new InvalidUrlException(ErrorCode.URL_INVALID_PROTOCOL);
    }

    public static InvalidUrlException selfReference() {
        return new InvalidUrlException(ErrorCode.URL_SELF_REFERENCE);
    }

    public static InvalidUrlException invalidFormat() {
        return new InvalidUrlException(ErrorCode.URL_INVALID_FORMAT);
    }
}
