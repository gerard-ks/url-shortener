package org.urlshortener.urlshortener.exception.security;

import org.urlshortener.urlshortener.constants.ErrorCode;

public class UnauthorizedOperationException extends SecurityException {

    private UnauthorizedOperationException(String errorCode, Object... args) {
        super(errorCode, args);
    }

    public static UnauthorizedOperationException invalidToken() {
        return new UnauthorizedOperationException(ErrorCode.TOKEN_MISMATCH);
    }

    public static UnauthorizedOperationException missingToken() {
        return new UnauthorizedOperationException(ErrorCode.TOKEN_MISSING);
    }

    public static UnauthorizedOperationException accessDenied() {
        return new UnauthorizedOperationException(ErrorCode.ACCESS_DENIED);
    }
}