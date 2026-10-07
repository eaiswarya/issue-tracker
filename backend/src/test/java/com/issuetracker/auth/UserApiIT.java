package com.issuetracker.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.issuetracker.TestcontainersConfiguration;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserApiIT {

    private static final String ME = "/api/v1/users/me";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private UserRepository users;

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    private User ada;

    @BeforeEach
    void setUp() {
        refreshTokens.deleteAll();
        users.deleteAll();
        ada = users.save(new User("Ada Lovelace", "ada@example.com", passwordEncoder.encode("correct horse")));
    }

    @Test
    void returnsTheCurrentUserForAValidAccessToken() throws Exception {
        String accessToken = login().get("accessToken").asText();

        me("Bearer " + accessToken)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ada.getId()))
            .andExpect(jsonPath("$.name").value("Ada Lovelace"))
            .andExpect(jsonPath("$.email").value("ada@example.com"))
            .andExpect(jsonPath("$.systemRole").value("USER"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void rejectsARequestWithoutAToken() throws Exception {
        mvc.perform(get(ME)).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAMalformedToken() throws Exception {
        me("Bearer not.a.jwt").andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsATokenSignedWithAnotherSecret() throws Exception {
        SecretKeySpec otherKey = new SecretKeySpec(
            "another-secret-that-is-at-least-32-bytes-long".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder forger = new NimbusJwtEncoder(new ImmutableSecret<>(otherKey));

        me("Bearer " + token(forger, ada.getId(), Instant.now(), Duration.ofMinutes(15)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAnExpiredToken() throws Exception {
        Instant twentyMinutesAgo = Instant.now().minus(Duration.ofMinutes(20));

        me("Bearer " + token(jwtEncoder, ada.getId(), twentyMinutesAgo, Duration.ofMinutes(15)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsATokenForAUserThatNoLongerExists() throws Exception {
        String accessToken = login().get("accessToken").asText();
        refreshTokens.deleteAll();
        users.deleteAll();

        me("Bearer " + accessToken).andExpect(status().isUnauthorized());
    }

    @Test
    void otherApiRoutesNeedAValidTokenToGetPastSecurity() throws Exception {
        String accessToken = login().get("accessToken").asText();

        mvc.perform(get("/api/v1/projects")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/projects").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void refreshStillWorksWhenTheClientSendsAnExpiredAccessToken() throws Exception {
        String refreshToken = login().get("refreshToken").asText();
        String expired = token(jwtEncoder, ada.getId(), Instant.now().minus(Duration.ofHours(1)), Duration.ofMinutes(15));

        mvc.perform(post("/api/v1/auth/refresh")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expired)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new RefreshRequest(refreshToken))))
            .andExpect(status().isOk());
    }

    private JsonNode login() throws Exception {
        String body = json.writeValueAsString(new LoginRequest("ada@example.com", "correct horse"));
        String response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    private ResultActions me(String authorization) throws Exception {
        return mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, authorization));
    }

    private static String token(JwtEncoder encoder, Long userId, Instant issuedAt, Duration ttl) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(JwtConfig.ISSUER)
            .subject(userId.toString())
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(ttl))
            .claim("role", "USER")
            .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(JwtConfig.ALGORITHM).build(), claims))
            .getTokenValue();
    }
}
