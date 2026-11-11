package com.paysim.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditEventRepository extends JpaRepository<PaymentAuditEvent, Long> {

    List<PaymentAuditEvent> findByPayment_IdOrderByCreatedAtAscIdAsc(UUID paymentId);

    @Query("""
            select new com.paysim.audit.RecentActivity(
                e.eventType, p.id, p.orderReference, e.previousStatus, e.newStatus, e.message, e.createdAt)
            from PaymentAuditEvent e join e.payment p
            order by e.createdAt desc, e.id desc
            """)
    List<RecentActivity> findRecent(Pageable pageable);
}
