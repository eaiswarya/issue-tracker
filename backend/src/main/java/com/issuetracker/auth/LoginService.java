package com.issuetracker.auth;

import java.time.Clock;
import java.util.Locale;
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
    }

    @Transactional
    public IssuedTokens login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = users.findForLoginByEmail(email).orElseThrow();
        passwordEncoder.matches(request.password(), user.getPasswordHash());
        return tokenService.issue(user);
    }
}
