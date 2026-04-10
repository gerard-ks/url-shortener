package org.urlshortener.urlshortener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.urlshortener.urlshortener.entity.AccessLog;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {
    //  STATISTIQUES PAR URL

    // Compte le nombre total d'accès pour une URL
    long countByShortenedUrlId(UUID urlId);

    // Récupère la date du dernier accès pour une URL
    @Query("SELECT MAX(al.accessedAt) FROM AccessLog al " +
            "WHERE al.shortenedUrl.id = :urlId")
    Instant findLastAccessedAtByUrlId(@Param("urlId") UUID urlId);

    //  STATISTIQUES AVANCÉES

    // Compte les accès sur une période donnée
    @Query("SELECT COUNT(al) FROM AccessLog al " +
            "WHERE al.shortenedUrl.id = :urlId " +
            "AND al.accessedAt BETWEEN :start AND :end")
    long countByUrlIdAndPeriod(
            @Param("urlId") UUID urlId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    //  Supprime tous les logs d'une URL
    void deleteByShortenedUrlId(UUID urlId);
}
