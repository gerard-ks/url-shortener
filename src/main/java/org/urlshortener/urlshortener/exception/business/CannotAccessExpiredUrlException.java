package org.urlshortener.urlshortener.exception.business;

import org.urlshortener.urlshortener.constants.ErrorCode;

public class CannotAccessExpiredUrlException extends BusinessRuleException {

    public CannotAccessExpiredUrlException(String shortCode) {
        super(ErrorCode.CANNOT_ACCESS_EXPIRED_URL, shortCode);
    }
}
