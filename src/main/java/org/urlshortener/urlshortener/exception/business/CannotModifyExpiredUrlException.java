package org.urlshortener.urlshortener.exception.business;

import org.urlshortener.urlshortener.constants.ErrorCode;

public class CannotModifyExpiredUrlException extends BusinessRuleException {
    public CannotModifyExpiredUrlException(String shortCode) {
        super(ErrorCode.CANNOT_MODIFY_EXPIRED_URL, shortCode);
    }
}
