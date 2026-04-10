package org.urlshortener.urlshortener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.urlshortener.urlshortener.entity.ShortenedUrl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShortenedUrlRepository extends JpaRepository<ShortenedUrl, UUID> {

    //  RECHERCHE PAR SHORT CODE

    Optional<ShortenedUrl> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);


    //  CLEANUP — Nettoyage URLs expirées (batch)


    /**
     * Compte les URLs expirées avant un seuil.
     * Utilisé pour les métriques avant suppression.
     */
    @Query("SELECT COUNT(u) FROM ShortenedUrl u " +
            "WHERE u.expiresAt IS NOT NULL " +
            "AND u.expiresAt < :threshold")
    long countExpiredBefore(@Param("threshold") Instant threshold);

    /**
     * Récupère les IDs des URLs expirées par batch.
     * Triées par date d'expiration (les plus anciennes d'abord).
     */
    @Query("SELECT u.id FROM ShortenedUrl u " +
            "WHERE u.expiresAt IS NOT NULL " +
            "AND u.expiresAt < :threshold " +
            "ORDER BY u.expiresAt ASC " +
            "LIMIT :limit")
    List<UUID> findExpiredIdsBefore(@Param("threshold") Instant threshold,
                                    @Param("limit") int limit);

    /**
     * Supprime les URLs par liste d'IDs.
     * CASCADE DELETE supprime automatiquement les access_logs.
     */
    @Modifying
    @Query("DELETE FROM ShortenedUrl u WHERE u.id IN :ids")
    int deleteByIds(@Param("ids") List<UUID> ids);

    //  STATISTIQUES GLOBALES (monitoring)


    @Query("SELECT COUNT(u) FROM ShortenedUrl u " +
            "WHERE u.expiresAt IS NULL " +
            "OR u.expiresAt > :now")
    long countActiveUrls(@Param("now") Instant now);

    @Query("SELECT COUNT(u) FROM ShortenedUrl u " +
            "WHERE u.expiresAt IS NOT NULL " +
            "AND u.expiresAt <= :now")
    long countExpiredUrls(@Param("now") Instant now);


    //  STATS — Projection avec JOIN

    @Query("""
        SELECT
            u.id as id,
            u.shortCode as shortCode,
            u.url as url,
            u.audit.createdAt as createdAt,
            u.audit.updatedAt as updatedAt,
            u.expiresAt as expiresAt,
            COUNT(al.id) as accessCount,
            MAX(al.accessedAt) as lastAccessedAt
        FROM ShortenedUrl u
        LEFT JOIN AccessLog al ON al.shortenedUrl.id = u.id
        WHERE u.shortCode = :shortCode
        GROUP BY u.id, u.shortCode, u.url, u.audit.createdAt, u.audit.updatedAt, u.expiresAt
    """)
    Optional<UrlWithStatsProjection> findWithStatsByShortCode(@Param("shortCode") String shortCode);
}
