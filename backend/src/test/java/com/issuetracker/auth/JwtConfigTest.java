package com.issuetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtConfigTest {

    @Test
    void mapsTheRoleClaimToASpringRole() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .subject("42")
            .claim("role", "ADMIN")
            .issuedAt(Instant.now())
            .build();

        var authentication = new JwtConfig().jwtAuthenticationConverter().convert(jwt);

        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
        assertThat(authentication.getName()).isEqualTo("42");
    }
}
