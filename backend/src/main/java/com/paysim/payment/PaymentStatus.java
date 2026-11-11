package com.paysim.payment;

/**
 * Lifecycle states of a simulated payment.
 *
 * <p>Permitted transitions (everything else is rejected):
 * <pre>
 *   SUCCEEDED -> REFUNDED
 *   DECLINED  -> (terminal)
 *   REFUNDED  -> (terminal)
 * </pre>
 * Payments are created directly in SUCCEEDED or DECLINED by the simulator.
 */
public enum PaymentStatus {
    SUCCEEDED,
    DECLINED,
    REFUNDED;

    public boolean canTransitionTo(PaymentStatus target) {
        return switch (this) {
            case SUCCEEDED -> target == REFUNDED;
            case DECLINED, REFUNDED -> false;
        };
    }
}
