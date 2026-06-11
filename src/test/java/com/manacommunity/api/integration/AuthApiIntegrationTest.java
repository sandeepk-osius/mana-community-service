package com.manacommunity.api.integration;

import com.manacommunity.api.dto.LoginRequest;
import com.manacommunity.api.dto.RegisterRequest;
import com.manacommunity.api.response.AuthResponse;
import com.manacommunity.api.support.BaseIntegrationTest;
import com.manacommunity.api.support.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack integration test for the Auth API.
 * Boots the complete Spring context against a TestContainers PostgreSQL.
 * mock-auth-enabled=true, so any background service calls succeed.
 *
 * Run: mvn failsafe:integration-test  (or -Dgroups=integration)
 */
@DisplayName("Auth API — Integration")
class AuthApiIntegrationTest extends BaseIntegrationTest {

    // ── POST /api/auth/register ───────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("full registration flow returns 201 with token")
        void successfulRegistration() {
            // Seed a community first via the admin seed endpoint (mock-auth-enabled=true)
            restTemplate.postForEntity(baseUrl("/api/admin/seed/all"), null, Void.class);

            RegisterRequest req = TestDataBuilder.registerRequest();
            // Use a unique email/phone to avoid conflicts across test runs
            req.setEmail("integration_" + System.currentTimeMillis() + "@test.com");
            req.setPhone("9" + (System.currentTimeMillis() % 1_000_000_000L));

            ResponseEntity<AuthResponse> response =
                    restTemplate.postForEntity(baseUrl("/api/auth/register"), req, AuthResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getToken()).isNotBlank();
        }

        @Test
        @DisplayName("duplicate email returns 409")
        void duplicateEmailReturns409() {
            restTemplate.postForEntity(baseUrl("/api/admin/seed/all"), null, Void.class);

            RegisterRequest req = TestDataBuilder.registerRequest();
            req.setEmail("dup_email_test@test.com");
            req.setPhone("8100000001");

            restTemplate.postForEntity(baseUrl("/api/auth/register"), req, AuthResponse.class);

            req.setPhone("8100000002"); // different phone, same email
            ResponseEntity<String> response =
                    restTemplate.postForEntity(baseUrl("/api/auth/register"), req, String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }

    // ── POST /api/auth/login ──────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("wrong password returns 401")
        void wrongPasswordReturns401() {
            restTemplate.postForEntity(baseUrl("/api/admin/seed/all"), null, Void.class);

            LoginRequest req = TestDataBuilder.loginRequest("admin@manacommunity.com", "wrong_password");
            ResponseEntity<String> response =
                    restTemplate.postForEntity(baseUrl("/api/auth/login"), req, String.class);

            assertThat(response.getStatusCode().value()).isIn(400, 401, 403);
        }
    }

    // ── Smoke: community list is reachable without auth ───────────────

    @Test
    @DisplayName("GET /api/communities is publicly accessible")
    void publicCommunityEndpoint() {
        ResponseEntity<String> response =
                restTemplate.getForEntity(baseUrl("/api/communities"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
