package com.issuetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.issuetracker.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerIT {

    private static final String REGISTER = "/api/v1/auth/register";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUp() {
        users.deleteAll();
    }

    @Test
    void registersUserWithoutAuthenticationAndReturnsItWithoutThePasswordHash() throws Exception {
        register("Ada Lovelace", "Ada@Example.com", "correct horse")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.name").value("Ada Lovelace"))
            .andExpect(jsonPath("$.email").value("ada@example.com"))
            .andExpect(jsonPath("$.systemRole").value("USER"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void storesThePasswordAsABcryptHash() throws Exception {
        register("Ada", "ada@example.com", "correct horse").andExpect(status().isCreated());

        String hash = users.findAll().getFirst().getPasswordHash();
        assertThat(hash).startsWith("$2").doesNotContain("correct horse");
        assertThat(passwordEncoder.matches("correct horse", hash)).isTrue();
    }

    @Test
    void rejectsADuplicateEmailIgnoringCaseWithConflict() throws Exception {
        register("Ada", "ada@example.com", "correct horse").andExpect(status().isCreated());

        register("Ada Again", "ADA@EXAMPLE.COM", "another password")
            .andExpect(status().isConflict())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.errors.email").value("An account with this email already exists"));
        assertThat(users.count()).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "blank name      | ''      | ada@example.com | correct horse | name",
        "missing email   | Ada     | ''              | correct horse | email",
        "malformed email | Ada     | not-an-email    | correct horse | email",
        "short password  | Ada     | ada@example.com | 1234567       | password",
    })
    void rejectsInvalidInputWithFieldErrors(String scenario, String name, String email, String password, String field)
            throws Exception {
        register(name, email, password)
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors." + field).isNotEmpty());
        assertThat(users.count()).isZero();
    }

    @Test
    void rejectsAPasswordLongerThanBcryptCanHash() throws Exception {
        // 37 two-byte characters = 74 bytes: under 72 characters but over BCrypt's 72-byte limit.
        register("Ada", "ada@example.com", "é".repeat(37))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.password").isNotEmpty());
    }

    @Test
    void acceptsAPasswordOfExactlyEightCharacters() throws Exception {
        register("Ada", "ada@example.com", "12345678").andExpect(status().isCreated());
    }

    private ResultActions register(String name, String email, String password) throws Exception {
        String body = """
            {"name": "%s", "email": "%s", "password": "%s"}
            """.formatted(name, email, password);
        return mvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
