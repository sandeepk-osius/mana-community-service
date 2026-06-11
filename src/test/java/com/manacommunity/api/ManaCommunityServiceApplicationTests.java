package com.manacommunity.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test: verifies the full Spring context loads without errors.
 * Uses TestContainers PostgreSQL so the real datasource config is validated.
 * Tagged "integration" so it only runs with mvn failsafe:integration-test.
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(properties = {
    "app.security.mock-auth-enabled=true",
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:sql/init-schema.sql"
})
@DisplayName("Application Context Load")
class ManaCommunityServiceApplicationTests {

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username",  POSTGRES::getUsername);
        registry.add("spring.datasource.password",  POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "manacommunity");
    }

    @Test
    @DisplayName("Spring context loads successfully")
    void contextLoads() {
        // Passes if no exception is thrown during context startup
    }
}
