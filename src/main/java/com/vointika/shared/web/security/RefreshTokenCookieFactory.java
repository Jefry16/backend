package com.vointika.shared.web.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenCookieFactory {

    private final SecurityProperties.RefreshCookie config;

    public RefreshTokenCookieFactory(SecurityProperties properties) {
        this.config = properties.refreshCookie();
    }



    public ResponseCookie issue(String rawToken) {
        return ResponseCookie.from(config.name(), rawToken)
                .httpOnly(true)
                .secure(config.secure())
                .sameSite(config.sameSite())
                .path(config.path())
                .maxAge(config.maxAge())
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(config.name(), "")
                .httpOnly(true)
                .secure(config.secure())
                .sameSite(config.sameSite())
                .path(config.path())
                .maxAge(Duration.ZERO)
                .build();
    }
}
