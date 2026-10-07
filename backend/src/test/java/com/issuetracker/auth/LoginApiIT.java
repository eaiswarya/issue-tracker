package com.issuetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.http.MediaType;
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
