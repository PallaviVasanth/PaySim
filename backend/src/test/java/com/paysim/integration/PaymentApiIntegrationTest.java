package com.paysim.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class PaymentApiIntegrationTest extends AbstractIntegrationTest {

    // ---------- creation ----------

    @Test
    void createsSucceededPayment() throws Exception {
        mvc.perform(postPayment(newKey(), body("ORD-1", "49.90", "usd")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "false"))
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.amount").value(49.90))
                .andExpect(jsonPath("$.id").isNotEmpty());
        assertThat(count("payments")).isEqualTo(1);
        assertThat(count("payment_audit_events")).isEqualTo(1);
    }

    @Test
    void simulatesDeclinedPayment() throws Exception {
        mvc.perform(postPayment(newKey(), body("ORD-2", "10.13", "USD")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.declineReason").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    void rejectsNegativeAndZeroAmounts() throws Exception {
        for (String amount : new String[] {"-5.00", "0", "0.00"}) {
            mvc.perform(postPayment(newKey(), body("ORD-3", amount, "USD")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"));
        }
        assertThat(count("payments")).isZero();
    }

    @Test
    void rejectsMalformedAndExcessiveAmounts() throws Exception {
        mvc.perform(postPayment(newKey(), body("ORD-4", "\"abc\"", "USD")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        mvc.perform(postPayment(newKey(), body("ORD-4", "1000000.01", "USD")))
                .andExpect(status().isBadRequest());
        mvc.perform(postPayment(newKey(), body("ORD-4", "1.999", "USD")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidAndUnsupportedCurrencies() throws Exception {
        mvc.perform(postPayment(newKey(), body("ORD-5", "10.00", "US1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("currency"));
        mvc.perform(postPayment(newKey(), body("ORD-5", "10.00", "ZZZ")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_CURRENCY"));
        mvc.perform(postPayment(newKey(), body("ORD-5", "10.50", "JPY")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_AMOUNT_PRECISION"));
    }

    @Test
    void rejectsMissingRequiredFields() throws Exception {
        mvc.perform(postPayment(newKey(), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(4));
    }

    // ---------- idempotency ----------

    @Test
    void requiresIdempotencyKey() throws Exception {
        mvc.perform(asOperator(post("/api/v1/payments"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ORD-6", "10.00", "USD")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_IDEMPOTENCY_KEY"));
    }

    @Test
    void rejectsMalformedIdempotencyKey() throws Exception {
        mvc.perform(postPayment("bad key!", body("ORD-6", "10.00", "USD")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"));
    }

    @Test
    void repeatedIdenticalRequestReturnsSamePaymentWithoutDuplicating() throws Exception {
        String key = newKey();
        String payload = body("ORD-7", "25.00", "USD");

        String firstId = json.readTree(mvc.perform(postPayment(key, payload))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();

        mvc.perform(postPayment(key, payload))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(firstId));

        // Same business content with different formatting of the amount is still the same request.
        mvc.perform(postPayment(key, body("ORD-7", "25.0", "usd")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstId));

        assertThat(count("payments")).isEqualTo(1);
        assertThat(count("idempotency_keys")).isEqualTo(1);
        assertThat(count("payment_audit_events")).isEqualTo(1);
    }

    @Test
    void reusingKeyWithDifferentPayloadIsConflict() throws Exception {
        String key = newKey();
        mvc.perform(postPayment(key, body("ORD-8", "25.00", "USD"))).andExpect(status().isCreated());

        mvc.perform(postPayment(key, body("ORD-8", "26.00", "USD")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
        assertThat(count("payments")).isEqualTo(1);
    }

    // ---------- refunds ----------

    @Test
    void refundsSucceededPaymentAndRecordsAudit() throws Exception {
        String id = createPayment("ORD-9", "30.00").get("id").asText();

        mvc.perform(refund(id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.refundedAt").isNotEmpty());

        mvc.perform(asViewer(get("/api/v1/payments/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.status").value("REFUNDED"))
                .andExpect(jsonPath("$.auditTrail.length()").value(2))
                .andExpect(jsonPath("$.auditTrail[0].eventType").value("PAYMENT_CREATED"))
                .andExpect(jsonPath("$.auditTrail[1].eventType").value("PAYMENT_REFUNDED"))
                .andExpect(jsonPath("$.auditTrail[1].previousStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.auditTrail[1].newStatus").value("REFUNDED"));
    }

    @Test
    void declinedPaymentCannotBeRefunded() throws Exception {
        String id = createPayment("ORD-10", "10.13").get("id").asText();
        mvc.perform(refund(id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void duplicateRefundIsRejected() throws Exception {
        String id = createPayment("ORD-11", "30.00").get("id").asText();
        mvc.perform(refund(id)).andExpect(status().isOk());
        mvc.perform(refund(id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM payment_audit_events WHERE event_type = 'PAYMENT_REFUNDED'", Long.class)).isEqualTo(1);
    }

    @Test
    void unknownAndMalformedIdsAreHandled() throws Exception {
        mvc.perform(refund("3f2b8c1e-0000-4000-8000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
        mvc.perform(asOperator(get("/api/v1/payments/not-a-uuid")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        mvc.perform(asOperator(get("/api/v1/payments/3f2b8c1e-0000-4000-8000-000000000000")))
                .andExpect(status().isNotFound());
    }

    // ---------- security ----------

    @Test
    void protectedEndpointsRequireAuthenticationWithoutBrowserPopup() throws Exception {
        mvc.perform(get("/api/v1/payments"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void viewerCannotCreateOrRefund() throws Exception {
        mvc.perform(asViewer(post("/api/v1/payments"))
                        .header("Idempotency-Key", newKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ORD-12", "10.00", "USD")))
                .andExpect(status().isForbidden());
        String id = createPayment("ORD-13", "10.00").get("id").asText();
        mvc.perform(asViewer(post("/api/v1/payments/" + id + "/refund"))).andExpect(status().isForbidden());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(get("/api/v1/me").with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .httpBasic("operator", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void errorResponsesNeverLeakStackTraces() throws Exception {
        String response = mvc.perform(postPayment(newKey(), "{not json"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("Exception").doesNotContain("at com.");
    }

    // ---------- listing & dashboard ----------

    @Test
    void listsWithFilterSearchSortAndPagination() throws Exception {
        for (int i = 1; i <= 5; i++) {
            createPayment("ORD-LIST-" + i, i + ".00");
        }
        createPayment("ORD-DECLINED", "3.13");

        mvc.perform(asViewer(get("/api/v1/payments").param("size", "2").param("sort", "amount").param("direction", "asc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].amount").value(1.0));

        mvc.perform(asViewer(get("/api/v1/payments").param("status", "DECLINED")))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].orderReference").value("ORD-DECLINED"));

        mvc.perform(asViewer(get("/api/v1/payments").param("q", "ord-list-3")))
                .andExpect(jsonPath("$.totalElements").value(1));

        // LIKE wildcards in user input are treated literally.
        mvc.perform(asViewer(get("/api/v1/payments").param("q", "%")))
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(asViewer(get("/api/v1/payments").param("size", "5000")))
                .andExpect(jsonPath("$.size").value(100));

        mvc.perform(asViewer(get("/api/v1/payments").param("sort", "password")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT"));
        mvc.perform(asViewer(get("/api/v1/payments").param("status", "BOGUS")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchFindsPaymentByExactId() throws Exception {
        JsonNode created = createPayment("ORD-ID", "12.00");
        createPayment("ORD-OTHER", "13.00");
        mvc.perform(asViewer(get("/api/v1/payments").param("q", created.get("id").asText())))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].orderReference").value("ORD-ID"));
    }

    @Test
    void dashboardMetricsMatchPersistedData() throws Exception {
        createPayment("ORD-A", "10.00");
        createPayment("ORD-B", "20.00");
        createPayment("ORD-C", "5.13");
        String refundedId = createPayment("ORD-D", "40.00").get("id").asText();
        mvc.perform(refund(refundedId)).andExpect(status().isOk());

        mvc.perform(asViewer(get("/api/v1/dashboard/summary")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").value(4))
                .andExpect(jsonPath("$.successfulTransactions").value(2))
                .andExpect(jsonPath("$.declinedTransactions").value(1))
                .andExpect(jsonPath("$.refundedTransactions").value(1))
                .andExpect(jsonPath("$.statusDistribution.SUCCEEDED").value(2))
                .andExpect(jsonPath("$.recentActivity.length()").value(5))
                .andExpect(jsonPath("$.recentActivity[0].eventType").value("PAYMENT_REFUNDED"));
    }

    @Test
    void dashboardIsZeroedWhenEmpty() throws Exception {
        mvc.perform(asViewer(get("/api/v1/dashboard/summary")))
                .andExpect(jsonPath("$.totalTransactions").value(0))
                .andExpect(jsonPath("$.statusDistribution.REFUNDED").value(0))
                .andExpect(jsonPath("$.recentActivity.length()").value(0));
    }

    @Test
    void meEndpointReportsRoles() throws Exception {
        mvc.perform(asViewer(get("/api/v1/me")))
                .andExpect(jsonPath("$.username").value("viewer"))
                .andExpect(jsonPath("$.roles[0]").value("VIEWER"));
    }
}
