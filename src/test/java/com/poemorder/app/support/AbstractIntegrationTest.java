package com.poemorder.app.support;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * База для всех интеграционных тестов.
 *
 * <p>По умолчанию поднимает реальный PostgreSQL через Testcontainers — так
 * тесты работают на CI и у любого разработчика с Docker, без ручной настройки БД.
 *
 * <p>Если Docker недоступен, запусти с {@code -Dtest.db=local} — тесты пойдут
 * против внешнего Postgres (по умолчанию {@code jdbc:postgresql://localhost:5432/poemorder_test},
 * переопределяется через {@code -Dtest.db.url/username/password}).
 *
 * <p>В обоих случаях Flyway применяет миграции к чистой/тестовой схеме.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final boolean USE_LOCAL_DB =
            "local".equals(System.getProperty("test.db", System.getenv("TEST_DB")));

    static final PostgreSQLContainer<?> POSTGRES =
            USE_LOCAL_DB ? null : new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        if (USE_LOCAL_DB) {
            registry.add("spring.datasource.url",
                    () -> System.getProperty("test.db.url", "jdbc:postgresql://localhost:5432/poemorder_test"));
            registry.add("spring.datasource.username", () -> System.getProperty("test.db.username", "poems"));
            registry.add("spring.datasource.password", () -> System.getProperty("test.db.password", "poems"));
        } else {
            POSTGRES.start();
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        }
    }
}
