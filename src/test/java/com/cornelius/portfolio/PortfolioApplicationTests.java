package com.cornelius.portfolio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PortfolioApplicationTests {

    @Test
    void contextLoads() {
        // Requires a reachable Postgres instance (see docker-compose.yml).
        // Replaced with a Testcontainers-backed test once entities land in M1.
    }
}
