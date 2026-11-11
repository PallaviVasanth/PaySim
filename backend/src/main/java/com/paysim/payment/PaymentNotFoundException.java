package com.paysim.payment;

import com.paysim.common.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends ApiException {

    public PaymentNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "No payment exists with id " + id + ".");
    }
}
