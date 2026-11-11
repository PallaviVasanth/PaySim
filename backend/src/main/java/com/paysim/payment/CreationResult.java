package com.paysim.payment;

/** Outcome of a create call: the payment plus whether it was served from an earlier identical request. */
public record CreationResult(Payment payment, boolean replayed) {
}
