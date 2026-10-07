package com.issuetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.issuetracker.TestcontainersConfiguration;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RefreshApiIT {

    private static final String REFRESH = "/api/v1/auth/refresh";
    private static final String LOGOUT = "/api/v1/auth/logout";

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
    private JdbcTemplate jdbc;

    private User ada;

    @BeforeEach
    void setUp() {
        refreshTokens.deleteAll();
        users.deleteAll();
        ada = users.save(new User("Ada", "ada@example.com", passwordEncoder.encode("correct horse")));
    }

    @Test
    void exchangesAValidRefreshTokenForANewPairAndRejectsTheOldOneAfterwards() throws Exception {
        String original = loginAndGetRefreshToken();

        JsonNode rotated = json.readTree(refresh(original)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andReturn().getResponse().getContentAsString());
        String next = rotated.get("refreshToken").asText();
        assertThat(next).isNotEqualTo(original);

        refresh(original)
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void reusingARotatedTokenAlsoRevokesTheTokenThatReplacedIt() throws Exception {
        String original = loginAndGetRefreshToken();
        String next = json.readTree(refresh(original).andReturn().getResponse().getContentAsString())
            .get("refreshToken").asText();

        refresh(original).andExpect(status().isUnauthorized());

        refresh(next).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAnUnknownToken() throws Exception {
        refresh("not-a-real-token")
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Refresh token is invalid or expired."));
    }

    @Test
    void rejectsAnExpiredToken() throws Exception {
        String token = loginAndGetRefreshToken();
        jdbc.update("update refresh_tokens set expires_at = ?", Timestamp.from(Instant.now().minusSeconds(1)));

        refresh(token).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTheTokenOfAUserDeactivatedSinceLogin() throws Exception {
        String token = loginAndGetRefreshToken();
        jdbc.update("update users set active = false where id = ?", ada.getId());

        refresh(token).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAMissingToken() throws Exception {
        mvc.perform(post(REFRESH).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.refreshToken").isNotEmpty());
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        String token = loginAndGetRefreshToken();

        logout(token).andExpect(status().isNoContent());

        refresh(token).andExpect(status().isUnauthorized());
        assertThat(refreshTokens.findAll()).allSatisfy(stored -> assertThat(stored.isRevoked()).isTrue());
    }

    @Test
    void logoutIsIdempotentAndDoesNotRevealWhetherATokenExists() throws Exception {
        String token = loginAndGetRefreshToken();
        logout(token).andExpect(status().isNoContent());

        logout(token).andExpect(status().isNoContent());
        logout("not-a-real-token").andExpect(status().isNoContent());
    }

    @Test
    void logoutRequiresARefreshToken() throws Exception {
        mvc.perform(post(LOGOUT).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.refreshToken").isNotEmpty());
    }

    private ResultActions logout(String refreshToken) throws Exception {
        String body = json.writeValueAsString(new RefreshRequest(refreshToken));
        return mvc.perform(post(LOGOUT).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String loginAndGetRefreshToken() throws Exception {
        String body = json.writeValueAsString(new LoginRequest("ada@example.com", "correct horse"));
        String response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("refreshToken").asText();
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        String body = json.writeValueAsString(new RefreshRequest(refreshToken));
        return mvc.perform(post(REFRESH).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
