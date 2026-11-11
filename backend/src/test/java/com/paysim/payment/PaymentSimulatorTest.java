package com.paysim.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentSimulatorTest {

    private final PaymentSimulator simulator = new PaymentSimulator();

    @Test
    void ordinaryAmountSucceeds() {
        SimulationResult r = simulator.evaluate(new BigDecimal("49.90"));
        assertThat(r.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(r.declineReason()).isNull();
    }

    @Test
    void amountEndingInThirteenCentsIsDeclinedForFunds() {
        SimulationResult r = simulator.evaluate(new BigDecimal("10.13"));
        assertThat(r.status()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(r.declineReason()).isEqualTo("INSUFFICIENT_FUNDS");
    }

    @Test
    void amountEndingInFiftyOneCentsFailsRiskCheck() {
        assertThat(simulator.evaluate(new BigDecimal("7.51")).declineReason()).isEqualTo("RISK_CHECK_FAILED");
    }

    @Test
    void amountAboveLimitIsDeclined() {
        assertThat(simulator.evaluate(new BigDecimal("10000.01")).declineReason()).isEqualTo("LIMIT_EXCEEDED");
        assertThat(simulator.evaluate(new BigDecimal("10000.00")).status()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void wholeCurrencyUnitsSucceed() {
        assertThat(simulator.evaluate(new BigDecimal("5000")).status()).isEqualTo(PaymentStatus.SUCCEEDED);
    }
}
