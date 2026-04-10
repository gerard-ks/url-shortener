package org.urlshortener.urlshortener.event;

import java.util.UUID;

public record AccessLogEvent(
        UUID urlId,
        String shortCode
) {}
