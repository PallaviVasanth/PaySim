package com.paysim.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentTest {

    private Payment payment(SimulationResult outcome) {
        return Payment.create("cust_1", "ORD-1", new BigDecimal("20.00"), "USD", null, outcome);
    }

    @Test
    void succeededPaymentCanBeRefundedOnce() {
        Payment p = payment(SimulationResult.succeeded());
        p.refund();
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(p.getRefundedAt()).isNotNull();

        assertThatThrownBy(p::refund).isInstanceOf(InvalidPaymentStateException.class);
    }

    @Test
    void declinedPaymentCannotBeRefunded() {
        Payment p = payment(SimulationResult.declined("INSUFFICIENT_FUNDS"));
        assertThatThrownBy(p::refund)
                .isInstanceOf(InvalidPaymentStateException.class)
                .hasMessageContaining("DECLINED");
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(p.getRefundedAt()).isNull();
    }
}
