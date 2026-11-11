package com.paysim.payment;

import com.paysim.common.ApiException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Currency allow-list and per-currency precision rules. */
@Component
public class CurrencyPolicy {

    static final Set<String> SUPPORTED = Set.of("USD", "EUR", "GBP", "INR", "JPY", "CAD", "AUD");

    public String normalize(String raw) {
        String code = raw.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED.contains(code)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "UNSUPPORTED_CURRENCY",
                    "Currency " + code + " is not supported. Supported: " + String.join(", ", new java.util.TreeSet<>(SUPPORTED)) + ".");
        }
        return code;
    }

    /** Verifies the amount does not carry more decimals than the currency allows and returns it at scale 2. */
    public BigDecimal normalizeAmount(BigDecimal amount, String currency) {
        int allowed = Currency.getInstance(currency).getDefaultFractionDigits();
        if (amount.stripTrailingZeros().scale() > allowed) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_AMOUNT_PRECISION",
                    "Currency " + currency + " allows at most " + allowed + " decimal places.");
        }
        return amount.setScale(2, java.math.RoundingMode.UNNECESSARY);
    }
}
