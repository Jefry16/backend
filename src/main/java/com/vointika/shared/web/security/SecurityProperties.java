package com.vointika.shared.web.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(RefreshCookie refreshCookie) {

    public record RefreshCookie(
            String name,
            String path,
            boolean secure,
            String sameSite,
            Duration maxAge
    ) {}
}
