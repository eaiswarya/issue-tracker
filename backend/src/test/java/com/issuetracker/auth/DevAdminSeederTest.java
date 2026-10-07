package com.issuetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DevAdminSeederTest {

    @Mock
    private UserRepository users;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createsAnAdminWhenNoneExists() throws Exception {
        when(users.existsBySystemRole(SystemRole.ADMIN)).thenReturn(false);
        when(users.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("dev-admin-pass")).thenReturn("hashed");

        seeder("Admin@Example.com", "dev-admin-pass").run(new DefaultApplicationArguments());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getSystemRole()).isEqualTo(SystemRole.ADMIN);
        assertThat(saved.getValue().getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
    }

    @Test
    void doesNothingWhenAnAdminAlreadyExists() throws Exception {
        when(users.existsBySystemRole(SystemRole.ADMIN)).thenReturn(true);

        seeder("admin@example.com", "dev-admin-pass").run(new DefaultApplicationArguments());

        verify(users, never()).save(any());
    }

    @Test
    void doesNothingWhenTheEmailBelongsToAnExistingUser() throws Exception {
        when(users.existsBySystemRole(SystemRole.ADMIN)).thenReturn(false);
        when(users.existsByEmailIgnoreCase("admin@example.com")).thenReturn(true);

        seeder("admin@example.com", "dev-admin-pass").run(new DefaultApplicationArguments());

        verify(users, never()).save(any());
    }

    @Test
    void skipsSeedingWhenCredentialsAreNotConfigured() throws Exception {
        seeder("", "").run(new DefaultApplicationArguments());

        verifyNoInteractions(users, passwordEncoder);
    }

    @Test
    void onlyRunsInTheDevProfile() {
        assertThat(DevAdminSeeder.class.getAnnotation(Profile.class).value()).containsExactly("dev");
    }

    private DevAdminSeeder seeder(String email, String password) {
        return new DevAdminSeeder(users, passwordEncoder, "Admin", email, password);
    }
}
