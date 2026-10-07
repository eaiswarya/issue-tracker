package com.issuetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.issuetracker.TestcontainersConfiguration;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UserRepositoryIT {

    private static final Duration WINDOW = Duration.ofMinutes(15);

    @Autowired
    private UserRepository users;

    @BeforeEach
    void cleanUp() {
        users.deleteAll();
    }

    @Test
    void newUserGetsDefaultRoleActiveFlagAndCreatedAt() {
        User saved = users.saveAndFlush(new User("Ada Lovelace", "ada@example.com", "hash"));

        User found = users.findById(saved.getId()).orElseThrow();
        assertThat(found.getSystemRole()).isEqualTo(SystemRole.USER);
        assertThat(found.isActive()).isTrue();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getAvatarUrl()).isNull();
        assertThat(found.getFailedLoginCount()).isZero();
        assertThat(found.getFailedLoginWindowStart()).isNull();
        assertThat(found.getLockedUntil()).isNull();
    }

    @Test
    void persistsFailedLoginCountAndLock() {
        User user = new User("Ada", "ada@example.com", "hash");
        Instant start = Instant.parse("2026-01-01T10:00:00Z");
        for (int i = 0; i < 3; i++) {
            user.recordFailedLogin(start.plusSeconds(i), 5, WINDOW, WINDOW);
        }
        User saved = users.saveAndFlush(user);

        User found = users.findById(saved.getId()).orElseThrow();
        assertThat(found.getFailedLoginCount()).isEqualTo(3);
        assertThat(found.getFailedLoginWindowStart()).isEqualTo(start);
        assertThat(found.getLockedUntil()).isNull();

        found.recordFailedLogin(start.plusSeconds(3), 5, WINDOW, WINDOW);
        found.recordFailedLogin(start.plusSeconds(4), 5, WINDOW, WINDOW);
        users.saveAndFlush(found);

        User locked = users.findById(saved.getId()).orElseThrow();
        assertThat(locked.getLockedUntil()).isEqualTo(start.plusSeconds(4).plus(WINDOW));
        assertThat(locked.getFailedLoginCount()).isZero();
    }

    @Test
    void emailIsUniqueIgnoringCase() {
        users.saveAndFlush(new User("Ada", "ada@example.com", "hash"));

        assertThatThrownBy(() -> users.saveAndFlush(new User("Ada Again", "ADA@example.com", "hash")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsByEmailIgnoringCase() {
        users.saveAndFlush(new User("Ada", "ada@example.com", "hash"));

        assertThat(users.existsByEmailIgnoreCase("Ada@Example.com")).isTrue();
        assertThat(users.existsByEmailIgnoreCase("grace@example.com")).isFalse();
    }
}
