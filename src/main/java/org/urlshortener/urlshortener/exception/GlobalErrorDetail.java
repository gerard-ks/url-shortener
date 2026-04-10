package org.urlshortener.urlshortener.exception;

public record GlobalErrorDetail(
        String object,
        String message
) {}
