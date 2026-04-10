package org.urlshortener.urlshortener.constants;

public final class ErrorCode {
    private ErrorCode() {
        throw new UnsupportedOperationException("Utility class");
    }

    //  VALIDATION - Bean Validation (JSR-380)
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";
    public static final String MISSING_PARAMETER = "MISSING_PARAMETER";
    public static final String TYPE_MISMATCH = "TYPE_MISMATCH";

    //  URL - Validation
    public static final String URL_EMPTY = "URL_EMPTY";
    public static final String URL_TOO_LONG = "URL_TOO_LONG";
    public static final String URL_INVALID_PROTOCOL = "URL_INVALID_PROTOCOL";
    public static final String URL_SELF_REFERENCE = "URL_SELF_REFERENCE";
    public static final String URL_INVALID_FORMAT = "URL_INVALID_FORMAT";

    //  SHORT CODE - Validation
    public static final String SHORT_CODE_EMPTY = "SHORT_CODE_EMPTY";
    public static final String SHORT_CODE_TOO_SHORT = "SHORT_CODE_TOO_SHORT";
    public static final String SHORT_CODE_TOO_LONG = "SHORT_CODE_TOO_LONG";
    public static final String SHORT_CODE_INVALID_CHARACTERS = "SHORT_CODE_INVALID_CHARACTERS";
    public static final String SHORT_CODE_RESERVED = "SHORT_CODE_RESERVED";
    public static final String SHORT_CODE_ALREADY_EXISTS = "SHORT_CODE_ALREADY_EXISTS";

    //  TOKEN - Validation et Sécurité
    public static final String TOKEN_EMPTY = "TOKEN_EMPTY";
    public static final String TOKEN_INVALID_FORMAT = "TOKEN_INVALID_FORMAT";
    public static final String TOKEN_MISMATCH = "TOKEN_MISMATCH";
    public static final String TOKEN_MISSING = "TOKEN_MISSING";

    //  EXPIRATION — Validation
    public static final String EXPIRATION_IN_PAST = "EXPIRATION_IN_PAST";
    public static final String EXPIRATION_TOO_FAR = "EXPIRATION_TOO_FAR";
    public static final String EXPIRATION_INVALID_FORMAT = "EXPIRATION_INVALID_FORMAT";

    //  RESSOURCE - État
    public static final String URL_NOT_FOUND = "URL_NOT_FOUND";
    public static final String URL_EXPIRED = "URL_EXPIRED";

    //  OPÉRATIONS - Règles métier
    public static final String CANNOT_MODIFY_EXPIRED_URL = "CANNOT_MODIFY_EXPIRED_URL";
    public static final String CANNOT_ACCESS_EXPIRED_URL = "CANNOT_ACCESS_EXPIRED_URL";
    public static final String NO_FIELD_TO_UPDATE = "NO_FIELD_TO_UPDATE";

    //  SÉCURITÉ - Autorisation
    public static final String ACCESS_DENIED = "ACCESS_DENIED";

    //  RATE LIMITING
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String IP_BLOCKED = "IP_BLOCKED";

    //  SYSTÈME — Erreurs internes
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String CODE_GENERATION_FAILED = "CODE_GENERATION_FAILED";
}
