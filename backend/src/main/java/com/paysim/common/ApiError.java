package com.paysim.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** Uniform error envelope returned by every failing endpoint. Never contains stack traces. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldViolation> fieldErrors) {

    public static ApiError of(int status, String reason, String code, String message, String path) {
        return new ApiError(Instant.now(), status, reason, code, message, path, List.of());
    }
}
