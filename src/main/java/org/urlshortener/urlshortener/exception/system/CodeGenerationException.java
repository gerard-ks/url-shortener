package org.urlshortener.urlshortener.exception.system;

import org.urlshortener.urlshortener.constants.ErrorCode;
import org.urlshortener.urlshortener.exception.base.UrlShortenerException;

public class CodeGenerationException extends UrlShortenerException {

    public CodeGenerationException(int attempts) {
        super(ErrorCode.CODE_GENERATION_FAILED, attempts);
    }

    public CodeGenerationException(int attempts, Throwable cause) {
        super(ErrorCode.CODE_GENERATION_FAILED, cause, attempts);
    }
}
