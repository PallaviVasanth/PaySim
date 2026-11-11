package com.paysim.audit;

import com.paysim.payment.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

/** Read model for the dashboard activity feed. */
public record RecentActivity(
        AuditEventType eventType,
        UUID paymentId,
        String orderReference,
        PaymentStatus previousStatus,
        PaymentStatus newStatus,
        String message,
        Instant occurredAt) {
}
