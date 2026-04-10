package org.urlshortener.urlshortener.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.cleanup")
@Getter
@Setter
public class CleanupProperties {
    private boolean enabled = true;
    private int retentionDays = 30;
    private int batchSize = 1000;
}
