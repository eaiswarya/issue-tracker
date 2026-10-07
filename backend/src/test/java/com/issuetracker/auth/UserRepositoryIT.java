package com.issuetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.issuetracker.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UserRepositoryIT {

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
