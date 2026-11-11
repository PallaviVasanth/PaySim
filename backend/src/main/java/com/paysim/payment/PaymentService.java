package com.paysim.payment;

import com.paysim.audit.AuditEventRepository;
import com.paysim.audit.AuditEventResponse;
import com.paysim.audit.PaymentAuditEvent;
import com.paysim.common.ApiException;
import com.paysim.idempotency.IdempotencyConflictException;
import com.paysim.idempotency.IdempotencyRecord;
import com.paysim.idempotency.IdempotencyRepository;
import com.paysim.idempotency.RequestFingerprint;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    static final Pattern IDEMPOTENCY_KEY_FORMAT = Pattern.compile("^[A-Za-z0-9._:\\-]{8,128}$");
    static final int MAX_PAGE_SIZE = 100;
    private static final Map<String, String> SORTABLE = Map.of(
            "createdAt", "createdAt",
            "amount", "amount",
            "status", "status",
            "orderReference", "orderReference",
            "customerReference", "customerReference");

    private final PaymentRepository payments;
    private final AuditEventRepository audits;
    private final IdempotencyRepository idempotencyKeys;
    private final PaymentSimulator simulator;
    private final CurrencyPolicy currencyPolicy;
    private final TransactionTemplate tx;

    public PaymentService(PaymentRepository payments, AuditEventRepository audits,
                          IdempotencyRepository idempotencyKeys, PaymentSimulator simulator,
                          CurrencyPolicy currencyPolicy, PlatformTransactionManager txManager) {
        this.payments = payments;
        this.audits = audits;
        this.idempotencyKeys = idempotencyKeys;
        this.simulator = simulator;
        this.currencyPolicy = currencyPolicy;
        this.tx = new TransactionTemplate(txManager);
    }

    /**
     * Creates a payment exactly once per Idempotency-Key.
     *
     * <p>Strategy: (1) look the key up and replay or reject on mismatch; (2) otherwise insert the payment,
     * its audit event and the idempotency row in ONE transaction. The unique constraint on the key column
     * is the final arbiter: if two identical requests race, one commit wins, the other transaction rolls
     * back entirely (no orphan payment) and then replays the winner's result.
     */
    public CreationResult createPayment(String idempotencyKey, CreatePaymentRequest request) {
        validateKey(idempotencyKey);

        String currency = currencyPolicy.normalize(request.currency());
        BigDecimal amount = currencyPolicy.normalizeAmount(request.amount(), currency);
        String customer = request.customerReference().trim();
        String order = request.orderReference().trim();
        String description = blankToNull(request.description());
        String fingerprint = RequestFingerprint.of(customer, order, amount, currency, description);

        Optional<CreationResult> existing = replay(idempotencyKey, fingerprint);
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            Payment created = tx.execute(status -> insertNew(idempotencyKey, fingerprint,
                    customer, order, amount, currency, description));
            return new CreationResult(created, false);
        } catch (DataIntegrityViolationException race) {
            // Another request with the same key committed first.
            return replay(idempotencyKey, fingerprint).orElseThrow(() -> race);
        }
    }

    private Payment insertNew(String key, String fingerprint, String customer, String order,
                              BigDecimal amount, String currency, String description) {
        Payment payment = Payment.create(customer, order, amount, currency, description,
                simulator.evaluate(amount));
        payments.save(payment);
        audits.save(PaymentAuditEvent.created(payment));
        // Flush so a duplicate-key violation surfaces here, inside the transaction, and rolls everything back.
        idempotencyKeys.saveAndFlush(new IdempotencyRecord(key, fingerprint, payment.getId()));
        log.info("Payment created id={} status={} currency={}", payment.getId(), payment.getStatus(), currency);
        return payment;
    }

    private Optional<CreationResult> replay(String key, String fingerprint) {
        return idempotencyKeys.findByKeyValue(key).map(record -> {
            if (!record.getRequestHash().equals(fingerprint)) {
                throw new IdempotencyConflictException();
            }
            Payment payment = payments.findById(record.getPaymentId())
                    .orElseThrow(() -> new PaymentNotFoundException(record.getPaymentId()));
            return new CreationResult(payment, true);
        });
    }

    /**
     * Full refund. The row is locked with SELECT ... FOR UPDATE, so of N concurrent refund requests
     * exactly one sees SUCCEEDED and applies the transition; the rest wait, then see REFUNDED and are
     * rejected with 409. The @Version column is a second line of defence against lost updates.
     */
    @Transactional
    public Payment refund(UUID id) {
        Payment payment = payments.findByIdForUpdate(id).orElseThrow(() -> new PaymentNotFoundException(id));
        PaymentStatus previous = payment.getStatus();
        payment.refund();
        audits.save(PaymentAuditEvent.refunded(payment, previous));
        log.info("Payment refunded id={}", id);
        return payment;
    }

    @Transactional(readOnly = true)
    public PaymentDetailResponse getDetail(UUID id) {
        Payment payment = payments.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
        var trail = audits.findByPayment_IdOrderByCreatedAtAscIdAsc(id).stream()
                .map(AuditEventResponse::from).toList();
        return new PaymentDetailResponse(PaymentResponse.from(payment), trail);
    }

    @Transactional(readOnly = true)
    public Page<Payment> search(String query, PaymentStatus status, String currency,
                                int page, int size, String sort, String direction) {
        String property = SORTABLE.get(sort);
        if (property == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SORT",
                    "Sort must be one of: " + String.join(", ", new java.util.TreeSet<>(SORTABLE.keySet())) + ".");
        }
        Sort.Direction dir;
        if ("asc".equalsIgnoreCase(direction)) {
            dir = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(direction)) {
            dir = Sort.Direction.DESC;
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SORT", "Direction must be 'asc' or 'desc'.");
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // 'id' tie-breaker keeps pagination stable when the sort column has duplicates.
        Sort ordering = Sort.by(dir, property).and(Sort.by(Sort.Direction.DESC, "id"));
        return payments.findAll(PaymentSpecifications.matching(query, status, currency),
                PageRequest.of(safePage, safeSize, ordering));
    }

    private static void validateKey(String key) {
        if (key == null || !IDEMPOTENCY_KEY_FORMAT.matcher(key).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key must be 8-128 characters: letters, digits, '.', '_', ':' or '-'.");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
