package com.paysim.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paysim.audit.AuditEventRepository;
import com.paysim.audit.PaymentAuditEvent;
import com.paysim.common.ApiException;
import com.paysim.idempotency.IdempotencyConflictException;
import com.paysim.idempotency.IdempotencyRecord;
import com.paysim.idempotency.IdempotencyRepository;
import com.paysim.idempotency.RequestFingerprint;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

class PaymentServiceTest {

    private PaymentRepository payments;
    private AuditEventRepository audits;
    private IdempotencyRepository keys;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        payments = mock(PaymentRepository.class);
        audits = mock(AuditEventRepository.class);
        keys = mock(IdempotencyRepository.class);
        service = new PaymentService(payments, audits, keys, new PaymentSimulator(),
                new CurrencyPolicy(), mock(PlatformTransactionManager.class));
    }

    private CreatePaymentRequest request(String amount) {
        return new CreatePaymentRequest("cust_1", "ORD-1", new BigDecimal(amount), "usd", "  Plan ");
    }

    @Test
    void rejectsMalformedIdempotencyKey() {
        assertThatThrownBy(() -> service.createPayment("short", request("10.00")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo("INVALID_IDEMPOTENCY_KEY");
    }

    @Test
    void rejectsNullIdempotencyKey() {
        assertThatThrownBy(() -> service.createPayment(null, request("10.00")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void replaysStoredPaymentWhenFingerprintMatches() {
        UUID paymentId = UUID.randomUUID();
        String fingerprint = RequestFingerprint.of("cust_1", "ORD-1", new BigDecimal("10.00"), "USD", "Plan");
        Payment stored = Payment.create("cust_1", "ORD-1", new BigDecimal("10.00"), "USD", "Plan",
                SimulationResult.succeeded());
        when(keys.findByKeyValue("key-12345678")).thenReturn(
                Optional.of(new IdempotencyRecord("key-12345678", fingerprint, paymentId)));
        when(payments.findById(paymentId)).thenReturn(Optional.of(stored));

        CreationResult result = service.createPayment("key-12345678", request("10.0"));

        assertThat(result.replayed()).isTrue();
        assertThat(result.payment()).isSameAs(stored);
        verify(payments, never()).save(any());
    }

    @Test
    void conflictsWhenKeyIsReusedWithDifferentPayload() {
        String otherFingerprint = RequestFingerprint.of("cust_1", "ORD-1", new BigDecimal("99.00"), "USD", "Plan");
        when(keys.findByKeyValue("key-12345678")).thenReturn(
                Optional.of(new IdempotencyRecord("key-12345678", otherFingerprint, UUID.randomUUID())));

        assertThatThrownBy(() -> service.createPayment("key-12345678", request("10.00")))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void refundAppliesTransitionAndWritesAuditEvent() {
        UUID id = UUID.randomUUID();
        Payment p = Payment.create("cust_1", "ORD-1", new BigDecimal("10.00"), "USD", null, SimulationResult.succeeded());
        when(payments.findByIdForUpdate(id)).thenReturn(Optional.of(p));

        Payment refunded = service.refund(id);

        assertThat(refunded.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(audits).save(any(PaymentAuditEvent.class));
    }

    @Test
    void refundOfUnknownPaymentIsNotFound() {
        UUID id = UUID.randomUUID();
        when(payments.findByIdForUpdate(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.refund(id)).isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void refundOfDeclinedPaymentWritesNoAuditEvent() {
        UUID id = UUID.randomUUID();
        Payment p = Payment.create("cust_1", "ORD-1", new BigDecimal("10.13"), "USD", null,
                SimulationResult.declined("INSUFFICIENT_FUNDS"));
        when(payments.findByIdForUpdate(id)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.refund(id)).isInstanceOf(InvalidPaymentStateException.class);
        verify(audits, never()).save(any());
    }

    @Test
    void searchRejectsUnknownSortField() {
        assertThatThrownBy(() -> service.search(null, null, null, 0, 20, "password", "asc"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo("INVALID_SORT");
    }
}
