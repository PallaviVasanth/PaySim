package com.paysim.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Boots the full application against a real PostgreSQL started by Testcontainers (Docker required).
 * The container is shared by every integration test class (singleton pattern) so the Spring context
 * can be cached.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE TABLE idempotency_keys, payment_audit_events, payments RESTART IDENTITY CASCADE");
    }

    protected static String newKey() {
        return "key-" + UUID.randomUUID();
    }

    protected static String body(String order, String amount, String currency) {
        return """
                {"customerReference":"cust_1","orderReference":"%s","amount":%s,"currency":"%s","description":"Test"}
                """.formatted(order, amount, currency);
    }

    protected MockHttpServletRequestBuilder asOperator(MockHttpServletRequestBuilder b) {
        return b.with(httpBasic("operator", "operator-test-pass"));
    }

    protected MockHttpServletRequestBuilder asViewer(MockHttpServletRequestBuilder b) {
        return b.with(httpBasic("viewer", "viewer-test-pass"));
    }

    protected MockHttpServletRequestBuilder postPayment(String key, String payload) {
        return asOperator(MockMvcRequestBuilders.post("/api/v1/payments"))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload);
    }

    protected JsonNode createPayment(String order, String amount) throws Exception {
        MvcResult r = mvc.perform(postPayment(newKey(), body(order, amount, "USD"))).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    protected MockHttpServletRequestBuilder refund(String id) {
        return asOperator(MockMvcRequestBuilders.post("/api/v1/payments/" + id + "/refund"));
    }

    protected long count(String table) {
        Long n = jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
        return n == null ? 0 : n;
    }
}
