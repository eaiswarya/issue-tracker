package com.issuetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
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

    @Test
    void rotationRevokesTheOldRefreshTokenAndIssuesANewPair() {
        RefreshToken old = stored("old-token", NOW.plus(Duration.ofDays(1)));

        IssuedTokens tokens = service.rotate("old-token");

        assertThat(old.getRevokedAt()).isEqualTo(NOW);
        assertThat(tokens.refreshToken()).isNotEqualTo("old-token");
        assertThat(decoder.decode(tokens.accessToken()).getSubject()).isEqualTo("42");
        verify(refreshTokens).save(any(RefreshToken.class));
    }

    @Test
    void rotationRejectsAnUnknownToken() {
        when(refreshTokens.findByTokenHashForUpdate(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("unknown")).isInstanceOf(InvalidRefreshTokenException.class);
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void rotationRejectsAnExpiredToken() {
        stored("expired", NOW);

        assertThatThrownBy(() -> service.rotate("expired")).isInstanceOf(InvalidRefreshTokenException.class);
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void reusingARevokedTokenRevokesEveryRefreshTokenOfTheUser() {
        stored("rotated", NOW.plus(Duration.ofDays(1))).revoke(NOW.minusSeconds(60));

        assertThatThrownBy(() -> service.rotate("rotated")).isInstanceOf(InvalidRefreshTokenException.class);
        verify(refreshTokens).revokeAllActiveForUser(42L, NOW);
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void rotationRejectsAndRevokesTheTokenOfADeactivatedUser() {
        RefreshToken token = stored("valid", NOW.plus(Duration.ofDays(1)));
        ReflectionTestUtils.setField(ada, "active", false);

        assertThatThrownBy(() -> service.rotate("valid")).isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void revokeMarksTheRefreshTokenRevoked() {
        RefreshToken token = stored("valid", NOW.plus(Duration.ofDays(1)));

        service.revoke("valid");

        assertThat(token.getRevokedAt()).isEqualTo(NOW);
    }

    @Test
    void revokeIgnoresAnUnknownToken() {
        when(refreshTokens.findByTokenHashForUpdate(any())).thenReturn(Optional.empty());

        service.revoke("unknown");

        verify(refreshTokens, never()).revokeAllActiveForUser(any(), any());
    }

    private RefreshToken stored(String rawToken, Instant expiresAt) {
        RefreshToken token = new RefreshToken(ada, TokenHashes.sha256Hex(rawToken), expiresAt, NOW.minusSeconds(3600));
        when(refreshTokens.findByTokenHashForUpdate(TokenHashes.sha256Hex(rawToken))).thenReturn(Optional.of(token));
        return token;
    }
}
