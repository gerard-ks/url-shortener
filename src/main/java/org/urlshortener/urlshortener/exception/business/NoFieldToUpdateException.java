package org.urlshortener.urlshortener.exception.business;

import org.urlshortener.urlshortener.constants.ErrorCode;

public class NoFieldToUpdateException extends BusinessRuleException {

    public NoFieldToUpdateException() {
        super(ErrorCode.NO_FIELD_TO_UPDATE);
    }
}
