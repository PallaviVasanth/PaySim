package com.paysim.audit;

import com.paysim.payment.PaymentStatus;
import java.time.Instant;

public record AuditEventResponse(
        long id,
        AuditEventType eventType,
        PaymentStatus previousStatus,
        PaymentStatus newStatus,
        String message,
        Instant occurredAt) {

    public static AuditEventResponse from(PaymentAuditEvent e) {
        return new AuditEventResponse(e.getId(), e.getEventType(), e.getPreviousStatus(),
                e.getNewStatus(), e.getMessage(), e.getCreatedAt());
    }
}
