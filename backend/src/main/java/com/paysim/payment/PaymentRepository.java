
package com.paysim.payment;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {

    /** Row-level write lock (SELECT ... FOR UPDATE) so concurrent refunds are serialised. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);

    @Query("select p.status as status, count(p) as total from Payment p group by p.status")
    List<StatusCount> countGroupedByStatus();

    /** Counts only payments explicitly marked as demo. */
    @Query("select p.status as status, count(p) as total from Payment p where p.demo = true group by p.status")
    List<StatusCount> countGroupedByStatusForDemo();

    /** Retrieves a demo payment by ID without exposing operator payments. */
    Optional<Payment> findByIdAndDemoTrue(UUID id);

    interface StatusCount {
        PaymentStatus getStatus();

        long getTotal();
    }
}
