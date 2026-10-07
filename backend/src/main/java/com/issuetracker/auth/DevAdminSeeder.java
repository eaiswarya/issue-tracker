package com.issuetracker.auth;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Creates one ADMIN user on startup in the dev profile, from ADMIN_EMAIL / ADMIN_PASSWORD. No credentials are committed.
 */
@Component
@Profile("dev")
public class DevAdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevAdminSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String name;
    private final String email;
    private final String password;

    public DevAdminSeeder(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.admin.name:Admin}") String name,
            @Value("${app.seed.admin.email:}") String email,
            @Value("${app.seed.admin.password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.name = name;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.warn("Dev admin not seeded: set ADMIN_EMAIL and ADMIN_PASSWORD to create one");
            return;
        }
        if (users.existsBySystemRole(SystemRole.ADMIN)) {
            return;
        }
        if (users.existsByEmailIgnoreCase(email)) {
            log.warn("Dev admin not seeded: {} is already registered as a regular user", email);
            return;
        }
        users.save(new User(name, email, passwordEncoder.encode(password), SystemRole.ADMIN));
        log.info("Seeded dev admin {}", email);
    }
}
