package com.paysim.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PaymentStatusTest {

    @Test
    void succeededCanOnlyBeRefunded() {
        assertThat(PaymentStatus.SUCCEEDED.canTransitionTo(PaymentStatus.REFUNDED)).isTrue();
        assertThat(PaymentStatus.SUCCEEDED.canTransitionTo(PaymentStatus.DECLINED)).isFalse();
        assertThat(PaymentStatus.SUCCEEDED.canTransitionTo(PaymentStatus.SUCCEEDED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(PaymentStatus.class)
    void declinedIsTerminal(PaymentStatus target) {
        assertThat(PaymentStatus.DECLINED.canTransitionTo(target)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(PaymentStatus.class)
    void refundedIsTerminal(PaymentStatus target) {
        assertThat(PaymentStatus.REFUNDED.canTransitionTo(target)).isFalse();
    }
}
