package com.paysim.payment;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for a payment processor. Outcomes depend only on the amount so that
 * demos and tests are reproducible:
 * <ul>
 *   <li>amount above 10,000.00 &rarr; DECLINED (LIMIT_EXCEEDED)</li>
 *   <li>amount ending in .13 &rarr; DECLINED (INSUFFICIENT_FUNDS)</li>
 *   <li>amount ending in .51 &rarr; DECLINED (RISK_CHECK_FAILED)</li>
 *   <li>everything else &rarr; SUCCEEDED</li>
 * </ul>
 */
@Component
public class PaymentSimulator {

    public static final BigDecimal DECLINE_ABOVE = new BigDecimal("10000.00");

    public SimulationResult evaluate(BigDecimal amount) {
        if (amount.compareTo(DECLINE_ABOVE) > 0) {
            return SimulationResult.declined("LIMIT_EXCEEDED");
        }
        int minorUnits = amount.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        return switch (minorUnits) {
            case 13 -> SimulationResult.declined("INSUFFICIENT_FUNDS");
            case 51 -> SimulationResult.declined("RISK_CHECK_FAILED");
            default -> SimulationResult.succeeded();
        };
    }
}
