package com.paysim.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** Exercises the race conditions the design claims to handle, against a real database. */
class ConcurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final int THREADS = 8;

    @Test
    void concurrentIdenticalCreateRequestsProduceExactlyOnePayment() throws Exception {
        String key = newKey();
        String payload = body("ORD-RACE", "75.00", "USD");

        List<Integer> statuses = runConcurrently(() ->
                mvc.perform(postPayment(key, payload)).andReturn().getResponse().getStatus());

        assertThat(statuses).containsOnly(200, 201);
        assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(1);
        assertThat(count("payments")).isEqualTo(1);
        assertThat(count("idempotency_keys")).isEqualTo(1);
        assertThat(count("payment_audit_events")).isEqualTo(1);
    }

    @Test
    void concurrentRefundsApplyExactlyOnce() throws Exception {
        String id = createPayment("ORD-REFUND-RACE", "60.00").get("id").asText();

        List<Integer> statuses = runConcurrently(() ->
                mvc.perform(refund(id)).andReturn().getResponse().getStatus());

        assertThat(statuses.stream().filter(s -> s == 200).count()).isEqualTo(1);
        assertThat(statuses.stream().filter(s -> s == 409).count()).isEqualTo(THREADS - 1);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM payment_audit_events WHERE event_type = 'PAYMENT_REFUNDED'", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM payments WHERE id = ?::uuid", String.class, id))
                .isEqualTo("REFUNDED");
    }

    private List<Integer> runConcurrently(Callable<Integer> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return task.call();
                }));
            }
            ready.await();
            go.countDown();
            List<Integer> results = new ArrayList<>();
            for (Future<Integer> f : futures) {
                results.add(f.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
