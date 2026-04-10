package org.urlshortener.urlshortener.service.implementation;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.urlshortener.urlshortener.dto.response.CreateUrlResponse;
import org.urlshortener.urlshortener.dto.response.UrlMetadataResponse;
import org.urlshortener.urlshortener.entity.ShortenedUrl;
import org.urlshortener.urlshortener.exception.business.NoFieldToUpdateException;
import org.urlshortener.urlshortener.exception.resource.UrlNotFoundException;
import org.urlshortener.urlshortener.exception.security.UnauthorizedOperationException;
import org.urlshortener.urlshortener.exception.system.CodeGenerationException;
import org.urlshortener.urlshortener.exception.validation.InvalidUrlException;
import org.urlshortener.urlshortener.exception.validation.ShortCodeAlreadyExistsException;
import org.urlshortener.urlshortener.mapper.ShortenMapper;
import org.urlshortener.urlshortener.repository.ShortenedUrlRepository;
import org.urlshortener.urlshortener.service.interfaces.CodeGenerator;
import org.urlshortener.urlshortener.service.interfaces.ShortenService;
import org.urlshortener.urlshortener.service.interfaces.TokenService;

import java.net.URI;
import java.time.Instant;

import static org.urlshortener.urlshortener.constants.Constants.*;

@Service
@Slf4j
@Transactional
public class ShortenServiceImpl implements ShortenService {

    private final ShortenedUrlRepository repository;
    private final CodeGenerator codeGenerator;
    private final TokenService tokenService;

    @Value("${app.base-url}")
    private String baseUrl;

    public ShortenServiceImpl(ShortenedUrlRepository repository, CodeGenerator codeGenerator, TokenService tokenService) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.tokenService = tokenService;
    }


    @Override
    public CreateUrlResponse create(String url, String customCode, Instant expiresAt) {
        log.info("Creating shortened URL for: {}", url);

        validateNotSelfReference(url);

        String token = tokenService.generateToken();
        String tokenHash = tokenService.hashToken(token);

        if (customCode != null && !customCode.isBlank()) {
            return createWithCustomCode(url, customCode, tokenHash, token, expiresAt);
        }

        return createWithGeneratedCode(url, tokenHash, token, expiresAt);
    }

    private CreateUrlResponse createWithCustomCode(String url, String customCode,
                                                   String tokenHash, String token,
                                                   Instant expiresAt) {
        ShortenedUrl entity = buildEntity(url, customCode, tokenHash, expiresAt);

        try {
            ShortenedUrl saved = repository.save(entity);
            log.info("URL created with custom code: {} -> {}", customCode, url);
            return ShortenMapper.toCreateResponse(saved, token, baseUrl);
        } catch (DataIntegrityViolationException e) {
            if (isShortCodeConstraintViolation(e)) {
                throw new ShortCodeAlreadyExistsException(customCode);
            }
            throw e;
        }
    }

    private CreateUrlResponse createWithGeneratedCode(String url, String tokenHash,
                                                      String token, Instant expiresAt) {
        int currentLength = SHORT_CODE_DEFAULT_LENGTH;
        int totalAttempts = 0;

        while (currentLength <= SHORT_CODE_MAX_LENGTH) {
            for (int attempt = 0; attempt < MAX_COLLISION_RETRIES; attempt++) {
                totalAttempts++;
                String shortCode = codeGenerator.generate(currentLength);

                ShortenedUrl entity = buildEntity(url, shortCode, tokenHash, expiresAt);

                try {
                    ShortenedUrl saved = repository.save(entity);
                    log.info("URL created successfully: {} -> {} (attempts: {})",
                            shortCode, url, totalAttempts);
                    return ShortenMapper.toCreateResponse(saved, token, baseUrl);
                } catch (DataIntegrityViolationException e) {
                    if (isShortCodeConstraintViolation(e)) {
                        log.debug("Collision on code '{}' (attempt {}/{})",
                                shortCode, attempt + 1, MAX_COLLISION_RETRIES);
                    } else {
                        throw e;
                    }
                }
            }

            currentLength++;
            log.warn("Increasing code length to {} after {} collisions",
                    currentLength, MAX_COLLISION_RETRIES);
        }

        throw new CodeGenerationException(totalAttempts);
    }

    private ShortenedUrl buildEntity(String url, String shortCode,
                                     String tokenHash, Instant expiresAt) {
        return expiresAt != null
                ? ShortenedUrl.create(url, shortCode, tokenHash, expiresAt)
                : ShortenedUrl.createWithDefaultExpiration(url, shortCode, tokenHash);
    }

    @Override
    public UrlMetadataResponse getMetadata(String shortCode) {
        ShortenedUrl url = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        return ShortenMapper.toMetadataResponse(url, baseUrl);
    }

    private void validateNotSelfReference(String url) {
        try {
            URI uri = URI.create(url);
            URI base = URI.create(baseUrl);
            String host = uri.getHost();
            String baseHost = base.getHost();

            if (host != null && host.equalsIgnoreCase(baseHost)) {
                throw InvalidUrlException.selfReference();
            }
        } catch (IllegalArgumentException e) {
            throw InvalidUrlException.invalidFormat();
        }
    }

    @Override
    @CacheEvict(value = "urls", key = "#shortCode")
    public UrlMetadataResponse update(String shortCode, String token,
                               String newUrl, Instant newExpiresAt) {
        log.info("Updating URL: {}", shortCode);

        ShortenedUrl url = authenticateAndFind(shortCode, token);

        if (newUrl == null && newExpiresAt == null) {
            throw new NoFieldToUpdateException();
        }

        if (newUrl != null) {
            validateNotSelfReference(newUrl);
            url.updateUrl(newUrl);
        }

        if (newExpiresAt != null) {
            url.updateExpiration(newExpiresAt);
        }

        ShortenedUrl updated = repository.save(url);
        log.info("URL updated successfully: {}", shortCode);

        return ShortenMapper.toMetadataResponse(updated, baseUrl);

    }

    @Override
    @CacheEvict(value = "urls", key = "#shortCode")
    public void delete(String shortCode, String token) {
        log.info("Deleting URL: {}", shortCode);

        ShortenedUrl url = authenticateAndFind(shortCode, token);

        repository.delete(url);

        log.info("URL deleted successfully: {}", shortCode);
    }

    private ShortenedUrl authenticateAndFind(String shortCode, String token) {
        ShortenedUrl url = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (!tokenService.verifyToken(token, url.getTokenHash())) {
            throw UnauthorizedOperationException.invalidToken();
        }

        return url;
    }

    private boolean isShortCodeConstraintViolation(DataIntegrityViolationException e) {
        Throwable cause = e.getCause();

        while (cause != null) {
            if (cause instanceof ConstraintViolationException cve) {
                return "uk_short_code".equalsIgnoreCase(cve.getConstraintName());
            }
            cause = cause.getCause();
        }
        return false;
    }
}
