package com.issuetracker.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthProperties properties;
    private final Clock clock;
    private final String dummyHash;

    public LoginService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            AuthProperties properties,
            Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.properties = properties;
        this.clock = clock;
        // Checked against for unknown emails so they cost the same BCrypt time as real accounts.
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * Failed attempts are committed even though the method throws, so the lockout count survives the request.
     */
    @Transactional(noRollbackFor = {InvalidCredentialsException.class, AccountLockedException.class})
    public IssuedTokens login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Optional<User> found = users.findForLoginByEmail(email);
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new InvalidCredentialsException();
        }
        User user = found.get();
        Instant now = clock.instant();
        if (user.isLockedAt(now)) {
            throw lockedUntil(user, now);
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            AuthProperties.Lockout lockout = properties.lockout();
            user.recordFailedLogin(now, lockout.maxAttempts(), lockout.window(), lockout.duration());
            if (user.isLockedAt(now)) {
                throw lockedUntil(user, now);
            }
            throw new InvalidCredentialsException();
        }
        if (!user.isActive()) {
            throw new AccountDeactivatedException();
        }
        user.recordSuccessfulLogin();
        return tokenService.issue(user);
    }

    private static AccountLockedException lockedUntil(User user, Instant now) {
        return new AccountLockedException(Duration.between(now, user.getLockedUntil()));
    }
}
