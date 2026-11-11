package com.paysim.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String customerReference,
        String orderReference,
        BigDecimal amount,
        String currency,
        String description,
        PaymentStatus status,
        String declineReason,
        Instant createdAt,
        Instant updatedAt,
        Instant refundedAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getCustomerReference(), p.getOrderReference(),
                p.getAmount(), p.getCurrency(), p.getDescription(), p.getStatus(), p.getDeclineReason(),
                p.getCreatedAt(), p.getUpdatedAt(), p.getRefundedAt());
    }
}
