package com.vointika.shared.media;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires {@link MediaUrlResolver} with the media bucket's public base URL. Every
 * media/avatar URL is built at read time against the current base, so changing
 * the asset domain never requires a data migration.
 */
@Configuration
public class MediaUrlConfig {

    @Bean
    public MediaUrlResolver mediaUrlResolver(
            @Value("${app.s3.public-base-url}") String publicBaseUrl) {
        return new MediaUrlResolver(publicBaseUrl);
    }
}
