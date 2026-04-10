package org.urlshortener.urlshortener.service.implementation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.urlshortener.urlshortener.dto.response.StatsResponse;
import org.urlshortener.urlshortener.exception.resource.UrlNotFoundException;
import org.urlshortener.urlshortener.mapper.ShortenMapper;
import org.urlshortener.urlshortener.repository.ShortenedUrlRepository;
import org.urlshortener.urlshortener.repository.UrlWithStatsProjection;
import org.urlshortener.urlshortener.service.interfaces.StatsService;


@Service
@Slf4j
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final ShortenedUrlRepository urlRepository;
    @Value("${app.base-url}")
    private String baseUrl;

    public StatsServiceImpl(ShortenedUrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }


    @Override
    public StatsResponse getStats(String shortCode) {
        UrlWithStatsProjection projection = urlRepository.findWithStatsByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        return ShortenMapper.toStatsResponse(projection, baseUrl);
    }
}

