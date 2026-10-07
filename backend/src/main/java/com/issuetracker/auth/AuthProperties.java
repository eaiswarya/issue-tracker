package com.issuetracker.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.lang.Nullable;

/**
 * Token lifetimes and lockout rules. The JWT secret comes from {@code JWT_SECRET} and has no default, so the app
 * refuses to start without one.
 */
@ConfigurationProperties("app.auth")
public record AuthProperties(
        @Nullable String jwtSecret,
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("7d") Duration refreshTokenTtl,
        @DefaultValue Lockout lockout) {

    static final int MIN_SECRET_BYTES = 32;

    public AuthProperties {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                "app.auth.jwt-secret (JWT_SECRET) must be set to at least " + MIN_SECRET_BYTES + " bytes");
        }
    }

    public record Lockout(
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("15m") Duration window,
        @DefaultValue("15m") Duration duration) {
    }
}
