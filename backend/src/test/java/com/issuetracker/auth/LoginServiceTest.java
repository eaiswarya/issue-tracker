package com.issuetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class LoginServiceTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-03-01T12:00:00Z"));
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final LoginService service =
        new LoginService(users, passwordEncoder, tokenService, TokenServiceTest.PROPERTIES, clock);

    private final IssuedTokens tokens = new IssuedTokens("access", "refresh", Duration.ofMinutes(15));
    private User ada;

    @BeforeEach
    void setUp() {
        ada = new User("Ada", "ada@example.com", "hash");
        when(users.findForLoginByEmail("ada@example.com")).thenReturn(Optional.of(ada));
        when(passwordEncoder.matches("correct horse", "hash")).thenReturn(true);
        when(tokenService.issue(ada)).thenReturn(tokens);
    }

    @Test
    void issuesTokensForTheRightPasswordLookingUpTheNormalisedEmail() {
        assertThat(service.login(new LoginRequest("  Ada@Example.COM ", "correct horse"))).isSameAs(tokens);
    }

    @Test
    void rejectsAWrongPassword() {
        assertThatThrownBy(() -> service.login(new LoginRequest("ada@example.com", "wrong")))
            .isInstanceOf(InvalidCredentialsException.class);
        verify(tokenService, never()).issue(any());
    }

    @Test
    void rejectsAnUnknownEmailTheSameWayAfterStillCheckingAPassword() {
        when(users.findForLoginByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("nobody@example.com", "whatever")))
            .isInstanceOf(InvalidCredentialsException.class);
        // Spends the same BCrypt time as a real check, so response timing does not reveal unknown emails.
        verify(passwordEncoder).matches(eq("whatever"), any());
    }
}
