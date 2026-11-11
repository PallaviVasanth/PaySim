package com.paysim.dashboard;

import com.paysim.audit.AuditEventRepository;
import com.paysim.payment.PaymentRepository;
import com.paysim.payment.PaymentStatus;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    static final int RECENT_ACTIVITY_LIMIT = 8;

    private final PaymentRepository payments;
    private final AuditEventRepository audits;

    public DashboardService(PaymentRepository payments, AuditEventRepository audits) {
        this.payments = payments;
        this.audits = audits;
    }

    /** Metrics come from one GROUP BY query and one LIMITed query; no full-table loads. */
    @Transactional(readOnly = true)
    public DashboardSummary summary() {
        Map<PaymentStatus, Long> counts = new EnumMap<>(PaymentStatus.class);
        for (PaymentStatus s : PaymentStatus.values()) {
            counts.put(s, 0L);
        }
        payments.countGroupedByStatus().forEach(row -> counts.put(row.getStatus(), row.getTotal()));

        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new DashboardSummary(
                total,
                counts.get(PaymentStatus.SUCCEEDED),
                counts.get(PaymentStatus.DECLINED),
                counts.get(PaymentStatus.REFUNDED),
                counts,
                audits.findRecent(PageRequest.of(0, RECENT_ACTIVITY_LIMIT)));
    }
}
