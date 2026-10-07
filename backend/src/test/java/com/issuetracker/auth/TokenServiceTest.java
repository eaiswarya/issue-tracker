package com.issuetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

class TokenServiceTest {

    static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");
    static final AuthProperties PROPERTIES = new AuthProperties(
        "unit-test-jwt-secret-0123456789-0123456789",
        Duration.ofMinutes(15),
        Duration.ofDays(7),
        new AuthProperties.Lockout(5, Duration.ofMinutes(15), Duration.ofMinutes(15)));

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final JwtConfig jwtConfig = new JwtConfig();
    private final JwtDecoder decoder = jwtConfig.jwtDecoder(PROPERTIES, clock);
    private final TokenService service =
        new TokenService(jwtConfig.jwtEncoder(PROPERTIES), refreshTokens, PROPERTIES, clock);

    private User ada;

    @BeforeEach
    void setUp() {
        ada = new User("Ada", "ada@example.com", "hash");
        ReflectionTestUtils.setField(ada, "id", 42L);
        when(refreshTokens.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void issuesAFifteenMinuteAccessTokenForTheUser() {
        IssuedTokens tokens = service.issue(ada);

        Jwt jwt = decoder.decode(tokens.accessToken());
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("ada@example.com");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(tokens.accessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void storesOnlyTheHashOfASevenDayRefreshToken() {
        IssuedTokens tokens = service.issue(ada);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens).save(saved.capture());
        assertThat(tokens.refreshToken()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(saved.getValue().getTokenHash())
            .isEqualTo(TokenHashes.sha256Hex(tokens.refreshToken()))
            .isNotEqualTo(tokens.refreshToken());
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        assertThat(saved.getValue().getUser()).isSameAs(ada);
    }

    @Test
    void issuesADifferentRefreshTokenEachTime() {
        assertThat(service.issue(ada).refreshToken()).isNotEqualTo(service.issue(ada).refreshToken());
    }
}
