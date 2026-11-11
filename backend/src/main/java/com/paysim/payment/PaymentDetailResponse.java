package com.paysim.payment;

import com.paysim.audit.AuditEventResponse;
import java.util.List;

public record PaymentDetailResponse(PaymentResponse payment, List<AuditEventResponse> auditTrail) {
}
