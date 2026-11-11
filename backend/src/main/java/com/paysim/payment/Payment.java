package com.paysim.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root for a simulated payment. State changes go through methods on this class so the
 * transition rules live in one place and cannot be bypassed by client input.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_reference", nullable = false, length = 64)
    private String customerReference;

    @Column(name = "order_reference", nullable = false, length = 64)
    private String orderReference;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "decline_reason", length = 64)
    private String declineReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    /** Optimistic lock: guards against lost updates if a row is modified outside the pessimistic path. */
    @Version
    private long version;

    protected Payment() {
        // required by JPA
    }

    public static Payment create(String customerReference, String orderReference, BigDecimal amount,
                                 String currency, String description, SimulationResult outcome) {
        Payment p = new Payment();
        p.customerReference = customerReference;
        p.orderReference = orderReference;
        p.amount = amount;
        p.currency = currency;
        p.description = description;
        p.status = outcome.status();
        p.declineReason = outcome.declineReason();
        return p;
    }

    /** Full refund. Only a SUCCEEDED payment may be refunded. */
    public void refund() {
        if (!status.canTransitionTo(PaymentStatus.REFUNDED)) {
            throw new InvalidPaymentStateException(status, PaymentStatus.REFUNDED);
        }
        this.status = PaymentStatus.REFUNDED;
        this.refundedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getCustomerReference() { return customerReference; }
    public String getOrderReference() { return orderReference; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
    public PaymentStatus getStatus() { return status; }
    public String getDeclineReason() { return declineReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getRefundedAt() { return refundedAt; }
    public long getVersion() { return version; }
}
