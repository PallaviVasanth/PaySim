package com.paysim.payment;

public record SimulationResult(PaymentStatus status, String declineReason) {

    public static SimulationResult succeeded() {
        return new SimulationResult(PaymentStatus.SUCCEEDED, null);
    }

    public static SimulationResult declined(String reason) {
        return new SimulationResult(PaymentStatus.DECLINED, reason);
    }
}
