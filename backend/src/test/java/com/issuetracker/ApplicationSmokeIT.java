package com.issuetracker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ApplicationSmokeIT {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void healthEndpointReportsUp() {
        ResponseEntity<Map> response = rest.getForEntity("/actuator/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
    }

    @Test
    void swaggerUiIsServed() {
        ResponseEntity<String> entry = rest.getForEntity("/swagger-ui.html", String.class);
        assertThat(entry.getStatusCode().is2xxSuccessful() || entry.getStatusCode().is3xxRedirection())
            .as("GET /swagger-ui.html returned %s", entry.getStatusCode())
            .isTrue();

        ResponseEntity<String> page = rest.getForEntity("/swagger-ui/index.html", String.class);
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page.getBody()).contains("Swagger UI");

        ResponseEntity<String> apiDocs = rest.getForEntity("/v3/api-docs", String.class);
        assertThat(apiDocs.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void flywayAppliesBaselineMigration() {
        Map<String, Object> row = jdbc.queryForMap(
            "select script, success from flyway_schema_history where version = '1'");

        assertThat(row).containsEntry("script", "V1__baseline.sql").containsEntry("success", true);
    }

    @Test
    void otherEndpointsRequireAuthentication() {
        ResponseEntity<String> response = rest.getForEntity("/api/v1/projects", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
