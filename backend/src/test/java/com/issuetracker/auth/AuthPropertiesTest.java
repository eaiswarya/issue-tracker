package com.issuetracker.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AuthPropertiesTest {

    private static final AuthProperties.Lockout LOCKOUT =
        new AuthProperties.Lockout(5, Duration.ofMinutes(15), Duration.ofMinutes(15));

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "only-31-bytes-long-secret-value")
    void refusesAMissingOrShortJwtSecretWithoutEchoingIt(String secret) {
        assertThatThrownBy(() -> properties(secret))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("JWT_SECRET")
            .satisfies(e -> {
                if (secret != null && !secret.isEmpty()) {
                    org.assertj.core.api.Assertions.assertThat(e.getMessage()).doesNotContain(secret);
                }
            });
    }

    @Test
    void acceptsAThirtyTwoByteSecret() {
        assertThatCode(() -> properties("x".repeat(32))).doesNotThrowAnyException();
    }

    private static AuthProperties properties(String secret) {
        return new AuthProperties(secret, Duration.ofMinutes(15), Duration.ofDays(7), LOCKOUT);
    }
}
