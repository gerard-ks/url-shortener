package org.urlshortener.urlshortener.constants;

import java.util.Set;

public final class Constants {
    private Constants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // URL
    public static final int URL_MAX_LENGTH = 2048;
    public static final String URL_PATTERN = "^https?://.*";

    // Short Code
    public static final int SHORT_CODE_DEFAULT_LENGTH = 6;
    public static final int SHORT_CODE_MIN_LENGTH = 4;
    public static final int SHORT_CODE_MAX_LENGTH = 20;
    public static final String SHORT_CODE_PATTERN = "^[a-zA-Z0-9_-]+$";

    // Expiration
    public static final long DEFAULT_EXPIRATION_DAYS = 365;
    public static final long MAX_EXPIRATION_YEARS = 5;

    // Code Generation
    public static final int MAX_COLLISION_RETRIES = 5;
    public static final String BASE62_ALPHABET =
            "abcdefghijklmnopqrstuvwxyz"
                    + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                    + "0123456789";

    // Reserved codes
    public static final Set<String> RESERVED_CODES = Set.of(
            "admin", "api", "stats", "r", "health",
            "docs", "dashboard", "manage"
    );

    //  TOKEN
    public static final String TOKEN_PREFIX = "tok_";
    public static final int TOKEN_LENGTH = 32;
    public static final String HASH_ALGORITHM = "SHA-256";
}
