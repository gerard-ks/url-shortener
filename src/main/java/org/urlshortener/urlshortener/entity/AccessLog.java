package org.urlshortener.urlshortener.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.urlshortener.urlshortener.exception.business.CannotAccessExpiredUrlException;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "access_logs", indexes = {
        @Index(name = "idx_access_logs_url_id", columnList = "url_id"),
        @Index(name = "idx_access_logs_accessed_at", columnList = "accessed_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString(of = {"id", "accessedAt"})
public class AccessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "url_id", nullable = false, foreignKey = @ForeignKey(
            name = "fk_access_log_shortened_url"
    ))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ShortenedUrl shortenedUrl;

    @Column(name = "accessed_at", nullable = false, updatable = false)
    private Instant accessedAt;

    private AccessLog(ShortenedUrl shortenedUrl, Instant accessedAt) {
        if (shortenedUrl == null) {
            throw new IllegalArgumentException("ShortenedUrl cannot be null");
        }
        if (shortenedUrl.isExpired()) {
            throw new CannotAccessExpiredUrlException(
                    "Cannot create access log for expired URL"
            );
        }
        if (accessedAt == null) {
            throw new IllegalArgumentException("AccessedAt cannot be null");
        }
        this.shortenedUrl = shortenedUrl;
        this.accessedAt = accessedAt;
    }

    public static AccessLog recordFor(ShortenedUrl shortenedUrl) {
        return new AccessLog(shortenedUrl, Instant.now());
    }

    public static AccessLog recordAt(ShortenedUrl shortenedUrl, Instant accessedAt) {
        return new AccessLog(shortenedUrl, accessedAt);
    }

    public UUID getUrlId() {
        return shortenedUrl.getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccessLog that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
