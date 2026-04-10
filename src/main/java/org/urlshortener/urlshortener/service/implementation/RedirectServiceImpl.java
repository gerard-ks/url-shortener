package org.urlshortener.urlshortener.service.implementation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.urlshortener.urlshortener.entity.ShortenedUrl;
import org.urlshortener.urlshortener.event.AccessEvent;
import org.urlshortener.urlshortener.event.AccessLogEvent;
import org.urlshortener.urlshortener.service.interfaces.RedirectService;
import org.urlshortener.urlshortener.service.interfaces.UrlCacheService;

@Service
@Slf4j
@Transactional(readOnly = true)
public class RedirectServiceImpl implements RedirectService {
    private final UrlCacheService urlCacheService;
    private final ApplicationEventPublisher eventPublisher;

    public RedirectServiceImpl(UrlCacheService urlCacheService, ApplicationEventPublisher eventPublisher) {
        this.urlCacheService = urlCacheService;
        this.eventPublisher = eventPublisher;
    }


    @Override
    public String redirectAndLog(String shortCode) {
        log.debug("Redirecting: {}", shortCode);

        ShortenedUrl url = urlCacheService.findActiveUrl(shortCode);

        eventPublisher.publishEvent(new AccessLogEvent(url.getId(), shortCode));
        eventPublisher.publishEvent(new AccessEvent(shortCode));

        log.info("Redirect: {} -> {}", shortCode, url.getUrl());

        return url.getUrl();
    }
}
