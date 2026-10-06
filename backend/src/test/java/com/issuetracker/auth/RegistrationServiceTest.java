package com.issuetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository users;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationService service;

    @Test
    void registersUserWithNormalisedEmailHashedPasswordAndUserRole() {
        when(users.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("correct horse")).thenReturn("hashed");
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = service.register(new RegisterRequest("  Ada Lovelace ", "  Ada@Example.COM ", "correct horse"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Ada Lovelace");
        assertThat(saved.getValue().getEmail()).isEqualTo("ada@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getValue().getSystemRole()).isEqualTo(SystemRole.USER);
        assertThat(user).isSameAs(saved.getValue());
    }

    @Test
    void rejectsAnEmailThatIsAlreadyRegistered() {
        when(users.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("Ada", "ADA@example.com", "correct horse")))
            .isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(users, never()).saveAndFlush(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void treatsAUniqueViolationFromAConcurrentSignUpAsDuplicate() {
        when(users.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("correct horse")).thenReturn("hashed");
        when(users.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("uq_users_email_lower"));

        assertThatThrownBy(() -> service.register(new RegisterRequest("Ada", "ada@example.com", "correct horse")))
            .isInstanceOf(EmailAlreadyRegisteredException.class);
    }
}
