package com.paysim.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreatePaymentRequest(
        @Schema(example = "cust_1042")
        @NotBlank(message = "Customer reference is required.")
        @Size(max = 64, message = "Customer reference must be at most 64 characters.")
        @Pattern(regexp = "^[A-Za-z0-9._:-]*$", message = "Use only letters, digits, '.', '_', ':' or '-'.")
        String customerReference,

        @Schema(example = "ORD-2026-0001")
        @NotBlank(message = "Order reference is required.")
        @Size(max = 64, message = "Order reference must be at most 64 characters.")
        @Pattern(regexp = "^[A-Za-z0-9._:-]*$", message = "Use only letters, digits, '.', '_', ':' or '-'.")
        String orderReference,

        @Schema(example = "49.90", description = "Positive amount, at most 2 decimals, up to 1,000,000.00")
        @NotNull(message = "Amount is required.")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01.")
        @DecimalMax(value = "1000000.00", message = "Amount must not exceed 1,000,000.00.")
        @Digits(integer = 7, fraction = 2, message = "Amount may have at most 2 decimal places.")
        BigDecimal amount,

        @Schema(example = "USD", description = "ISO 4217 code. Supported: USD, EUR, GBP, INR, JPY, CAD, AUD")
        @NotBlank(message = "Currency is required.")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter ISO 4217 code.")
        String currency,

        @Schema(example = "Annual plan")
        @Size(max = 255, message = "Description must be at most 255 characters.")
        String description) {
}
