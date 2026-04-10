package org.urlshortener.urlshortener.service.interfaces;

import org.urlshortener.urlshortener.dto.response.StatsResponse;

public interface StatsService {
    StatsResponse getStats(String shortCode);
}
