package org.urlshortener.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.urlshortener.urlshortener.exception.business.CannotModifyExpiredUrlException;
import org.urlshortener.urlshortener.exception.validation.InvalidExpirationException;
import org.urlshortener.urlshortener.exception.validation.InvalidShortCodeException;
import org.urlshortener.urlshortener.exception.validation.InvalidUrlException;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.urlshortener.urlshortener.constants.Constants.*;

@Entity
@Table(name = "shortened_urls",
        indexes = {
                @Index(name = "idx_expires_at", columnList = "expires_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_short_code", columnNames = "short_code"),
                @UniqueConstraint(name = "uk_token_hash", columnNames = "token_hash")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString(of = {"id", "shortCode", "url"})
public class ShortenedUrl {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Setter
    @Check(constraints = "length(url) <= " + URL_MAX_LENGTH)
    private String url;

    @Column(name = "short_code", nullable = false, length = SHORT_CODE_MAX_LENGTH)
    @Setter(AccessLevel.NONE)
    private String shortCode;

    @Column(name = "token_hash", nullable = false, length = 64)
    @Setter(AccessLevel.NONE)
    private String tokenHash;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
            @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
    })
    @Setter(AccessLevel.NONE)
    private AuditMetadata audit = new AuditMetadata();

    @Column(name = "expires_at")
    @Setter
    private Instant expiresAt;

    private ShortenedUrl(String url, String shortCode, String tokenHash, Instant expiresAt) {
        validateUrl(url);
        validateShortCode(shortCode);
        validateTokenHash(tokenHash);
        validateExpiration(expiresAt);

        this.url = url;
        this.shortCode = shortCode;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public static ShortenedUrl create(String url, String shortCode,
                                      String tokenHash, Instant expiresAt) {
        return new ShortenedUrl(url, shortCode, tokenHash, expiresAt);
    }

    public static ShortenedUrl createPermanent(String url, String shortCode,
                                               String tokenHash) {
        return new ShortenedUrl(url, shortCode, tokenHash, null);
    }

    public static ShortenedUrl createWithDefaultExpiration(String url, String shortCode,
                                                           String tokenHash) {
        Instant defaultExpiration = Instant.now().plusSeconds(365L * 24 * 60 * 60);
        return new ShortenedUrl(url, shortCode, tokenHash, defaultExpiration);
    }

    public void updateUrl(String newUrl) {
        assertNotExpired("Cannot modify URL of an expired short link");
        validateUrl(newUrl);
        this.url = newUrl;
    }

    public void updateExpiration(Instant newExpiration) {
        assertNotExpired("Cannot modify expiration of an expired short link");
        if (newExpiration != null && newExpiration.isBefore(Instant.now())) {
            throw InvalidExpirationException.inThePast();
        }
        this.expiresAt = newExpiration;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return !isExpired();
    }

    public Instant getCreatedAt() {
        return audit.getCreatedAt();
    }

    public Instant getUpdatedAt() {
        return audit.getUpdatedAt();
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw InvalidUrlException.empty();
        }
        if (url.length() > URL_MAX_LENGTH) {
            throw InvalidUrlException.tooLong(URL_MAX_LENGTH);
        }
        if (!url.matches(URL_PATTERN)) {
            throw InvalidUrlException.invalidProtocol();
        }

        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw InvalidUrlException.invalidFormat();
        }

        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw InvalidUrlException.invalidFormat();
        }
    }

    private void validateShortCode(String shortCode) {
        if (shortCode == null || shortCode.isBlank()) {
            throw InvalidShortCodeException.empty();
        }
        if (shortCode.length() < SHORT_CODE_MIN_LENGTH) {
            throw InvalidShortCodeException.tooShort(SHORT_CODE_MIN_LENGTH);
        }
        if (shortCode.length() > SHORT_CODE_MAX_LENGTH) {
            throw InvalidShortCodeException.tooLong(SHORT_CODE_MAX_LENGTH);
        }
        if (!shortCode.matches(SHORT_CODE_PATTERN)) {
            throw InvalidShortCodeException.invalidCharacters();
        }
        if (RESERVED_CODES.contains(shortCode.toLowerCase())) {
            throw InvalidShortCodeException.reserved(shortCode);
        }
    }

    private void validateTokenHash(String tokenHash) {
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException(
                    "TokenHash cannot be null or empty"
            );
        }
    }

    private void validateExpiration(Instant expiresAt) {
        if (expiresAt == null) return;

        Instant now = Instant.now();
        if (expiresAt.isBefore(now)) {
            throw InvalidExpirationException.inThePast();
        }

        Instant maxExpiration = now.plus(MAX_EXPIRATION_YEARS * DEFAULT_EXPIRATION_DAYS, ChronoUnit.DAYS);
        if (expiresAt.isAfter(maxExpiration)) {
            throw InvalidExpirationException.tooFarInFuture((int) MAX_EXPIRATION_YEARS);
        }
    }

    private void assertNotExpired(String message) {
        if (isExpired()) {
            throw new CannotModifyExpiredUrlException(message);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShortenedUrl that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
