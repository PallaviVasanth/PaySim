package com.paysim.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paysim.common.ApiException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CurrencyPolicyTest {

    private final CurrencyPolicy policy = new CurrencyPolicy();

    @Test
    void normalizesCaseAndWhitespace() {
        assertThat(policy.normalize(" usd ")).isEqualTo("USD");
    }

    @Test
    void rejectsUnsupportedCurrency() {
        assertThatThrownBy(() -> policy.normalize("ZZZ"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo("UNSUPPORTED_CURRENCY");
    }

    @Test
    void rejectsFractionsForZeroDecimalCurrency() {
        assertThatThrownBy(() -> policy.normalizeAmount(new BigDecimal("100.50"), "JPY"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo("INVALID_AMOUNT_PRECISION");
    }

    @Test
    void acceptsTrailingZerosForZeroDecimalCurrency() {
        assertThat(policy.normalizeAmount(new BigDecimal("100.00"), "JPY")).isEqualByComparingTo("100");
    }
}
