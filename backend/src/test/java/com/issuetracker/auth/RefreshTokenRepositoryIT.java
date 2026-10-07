package com.issuetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.issuetracker.TestcontainersConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RefreshTokenRepositoryIT {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @Autowired
    private UserRepository users;

    private User user;

    @BeforeEach
    void setUp() {
        refreshTokens.deleteAll();
        users.deleteAll();
        user = users.saveAndFlush(new User("Ada", "ada@example.com", "hash"));
    }

    @Test
    void storesTheHashAndExpiryAndFindsByHash() {
        refreshTokens.saveAndFlush(new RefreshToken(user, "a".repeat(64), NOW.plus(7, ChronoUnit.DAYS), NOW));

        RefreshToken found = refreshTokens.findByTokenHash("a".repeat(64)).orElseThrow();
        assertThat(found.getUser().getId()).isEqualTo(user.getId());
        assertThat(found.getExpiresAt()).isEqualTo(NOW.plus(7, ChronoUnit.DAYS));
        assertThat(found.getRevokedAt()).isNull();
    }

    @Test
    void tokenHashIsUnique() {
        refreshTokens.saveAndFlush(new RefreshToken(user, "a".repeat(64), NOW.plus(7, ChronoUnit.DAYS), NOW));

        assertThatThrownBy(() -> refreshTokens.saveAndFlush(
                new RefreshToken(user, "a".repeat(64), NOW.plus(7, ChronoUnit.DAYS), NOW)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void revokesAllActiveTokensOfAUser() {
        User other = users.saveAndFlush(new User("Grace", "grace@example.com", "hash"));
        refreshTokens.saveAndFlush(new RefreshToken(user, "a".repeat(64), NOW.plus(7, ChronoUnit.DAYS), NOW));
        refreshTokens.saveAndFlush(new RefreshToken(user, "b".repeat(64), NOW.plus(7, ChronoUnit.DAYS), NOW));
        refreshTokens.saveAndFlush(new RefreshToken(other, "c".repeat(64), NOW.plus(7, ChronoUnit.DAYS), NOW));

        int revoked = refreshTokens.revokeAllActiveForUser(user.getId(), NOW);

        assertThat(revoked).isEqualTo(2);
        assertThat(refreshTokens.findByTokenHash("a".repeat(64)).orElseThrow().getRevokedAt()).isEqualTo(NOW);
        assertThat(refreshTokens.findByTokenHash("c".repeat(64)).orElseThrow().getRevokedAt()).isNull();
    }

    @Test
    void deletingAUserDeletesTheirTokens() {
        refreshTokens.saveAndFlush(new RefreshToken(user, "a".repeat(64), NOW.plus(7, ChronoUnit.DAYS), NOW));

        users.deleteAll();

        assertThat(refreshTokens.count()).isZero();
    }
}
