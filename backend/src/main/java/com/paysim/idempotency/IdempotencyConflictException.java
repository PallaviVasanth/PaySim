package com.paysim.idempotency;

import com.paysim.common.ApiException;
import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends ApiException {

    public IdempotencyConflictException() {
        super(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                "This Idempotency-Key was already used with a different request body.");
    }
}
