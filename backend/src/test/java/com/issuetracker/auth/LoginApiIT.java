package com.issuetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.issuetracker.TestcontainersConfiguration;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LoginApiIT {

    private static final String LOGIN = "/api/v1/auth/login";

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
    private JwtDecoder jwtDecoder;

    @Autowired
    private JdbcTemplate jdbc;

    private User ada;

    @BeforeEach
    void setUp() {
        refreshTokens.deleteAll();
        users.deleteAll();
        ada = users.save(new User("Ada", "ada@example.com", passwordEncoder.encode("correct horse")));
    }

    @Test
    void returnsAFifteenMinuteAccessTokenAndASevenDayRefreshToken() throws Exception {
        String response = login("Ada@Example.com", "correct horse")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andReturn().getResponse().getContentAsString();

        JsonNode body = json.readTree(response);
        Jwt jwt = jwtDecoder.decode(body.get("accessToken").asText());
        assertThat(jwt.getSubject()).isEqualTo(ada.getId().toString());
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));

        String refreshToken = body.get("refreshToken").asText();
        RefreshToken stored = refreshTokens.findAll().getFirst();
        assertThat(stored.getTokenHash()).isEqualTo(TokenHashes.sha256Hex(refreshToken)).isNotEqualTo(refreshToken);
        assertThat(Duration.between(stored.getCreatedAt(), stored.getExpiresAt())).isEqualTo(Duration.ofDays(7));
    }

    @Test
    void answersAWrongPasswordAndAnUnknownEmailIdenticallyWithAGeneric401() throws Exception {
        String wrongPassword = login("ada@example.com", "wrong password")
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value("Invalid email or password."))
            .andReturn().getResponse().getContentAsString();
        String unknownEmail = login("nobody@example.com", "wrong password")
            .andExpect(status().isUnauthorized())
            .andReturn().getResponse().getContentAsString();

        assertThat(unknownEmail).isEqualTo(wrongPassword);
        assertThat(refreshTokens.count()).isZero();
    }

    @Test
    void treatsAPasswordLongerThanBcryptAcceptsAsWrongCredentials() throws Exception {
        login("ada@example.com", "é".repeat(40)).andExpect(status().isUnauthorized());
    }

    @Test
    void locksTheAccountAfterFiveFailedAttemptsAndKeepsItLockedForTheRightPassword() throws Exception {
        for (int i = 0; i < 4; i++) {
            login("ada@example.com", "wrong password").andExpect(status().isUnauthorized());
        }

        login("ada@example.com", "wrong password")
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string(HttpHeaders.RETRY_AFTER, "900"))
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value("Too many failed login attempts. Try again in 15 minutes."));
        login("ada@example.com", "correct horse").andExpect(status().isTooManyRequests());

        assertThat(users.findById(ada.getId()).orElseThrow().getLockedUntil()).isNotNull();
        assertThat(refreshTokens.count()).isZero();
    }

    @Test
    void refusesADeactivatedUser() throws Exception {
        jdbc.update("update users set active = false where id = ?", ada.getId());

        login("ada@example.com", "correct horse")
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value("This account has been deactivated."));
        login("ada@example.com", "wrong password").andExpect(status().isUnauthorized());
        assertThat(refreshTokens.count()).isZero();
    }

    @Test
    void rejectsAMissingEmailWithAFieldError() throws Exception {
        login("", "correct horse")
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errors.email").isNotEmpty());
    }

    private ResultActions login(String email, String password) throws Exception {
        String body = json.writeValueAsString(new LoginRequest(email, password));
        return mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
