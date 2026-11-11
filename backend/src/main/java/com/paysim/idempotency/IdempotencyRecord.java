package com.paysim.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 128, updatable = false)
    private String keyValue;

    @Column(name = "request_hash", nullable = false, length = 64, updatable = false)
    private String requestHash;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
        // required by JPA
    }

    public IdempotencyRecord(String keyValue, String requestHash, UUID paymentId) {
        this.keyValue = keyValue;
        this.requestHash = requestHash;
        this.paymentId = paymentId;
        this.createdAt = Instant.now();
    }

    public String getRequestHash() { return requestHash; }
    public UUID getPaymentId() { return paymentId; }
}
