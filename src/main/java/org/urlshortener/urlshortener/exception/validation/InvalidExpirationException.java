package org.urlshortener.urlshortener.exception.validation;

import org.urlshortener.urlshortener.constants.ErrorCode;

public class InvalidExpirationException extends ValidationException {

    private InvalidExpirationException(String errorCode, Object... args) {
        super(errorCode, args);
    }

    public static InvalidExpirationException inThePast() {
        return new InvalidExpirationException(ErrorCode.EXPIRATION_IN_PAST);
    }

    public static InvalidExpirationException tooFarInFuture(int maxYears) {
        return new InvalidExpirationException(ErrorCode.EXPIRATION_TOO_FAR, maxYears);
    }

    public static InvalidExpirationException invalidFormat() {
        return new InvalidExpirationException(ErrorCode.EXPIRATION_INVALID_FORMAT);
    }
}
