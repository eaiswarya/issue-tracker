package com.issuetracker.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Stateless API security. Health, API docs and the auth endpoints are public; everything else needs a valid JWT
 * access token in the {@code Authorization: Bearer} header.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error"
    };

    /**
     * Public because the caller authenticates with credentials or a refresh token in the body, not an access token.
     */
    private static final String AUTH_PREFIX = "/api/v1/auth/";
    private static final String[] AUTH_PATHS = {
        AUTH_PREFIX + "register", AUTH_PREFIX + "login", AUTH_PREFIX + "refresh", AUTH_PREFIX + "logout"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers(HttpMethod.POST, AUTH_PATHS)
                    .permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(resourceServer -> resourceServer
                .bearerTokenResolver(bearerTokenResolver())
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .build();
    }

    /**
     * Ignores the Authorization header on the auth endpoints, so a client holding an expired access token can still
     * refresh, log in or log out instead of being rejected before the controller runs.
     */
    private static BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver bearer = new DefaultBearerTokenResolver();
        return request -> request.getRequestURI().startsWith(request.getContextPath() + AUTH_PREFIX)
            ? null
            : bearer.resolve(request);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
