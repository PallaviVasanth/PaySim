package com.paysim.payment;

import com.paysim.common.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidPaymentStateException extends ApiException {

    public InvalidPaymentStateException(PaymentStatus from, PaymentStatus to) {
        super(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION",
                "Cannot move a payment from " + from + " to " + to + ".");
    }
}
