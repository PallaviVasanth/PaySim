package com.paysim.payment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Create, search and refund simulated payments")
public class PaymentController {

    public static final String REPLAY_HEADER = "Idempotent-Replayed";

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a simulated payment",
            description = "Requires an Idempotency-Key header. Repeating the same request with the same key "
                    + "returns the original payment (HTTP 200 + Idempotent-Replayed: true). Reusing a key with a "
                    + "different body returns 409.")
    @ApiResponse(responseCode = "201", description = "Payment created")
    @ApiResponse(responseCode = "200", description = "Replay of an earlier identical request")
    @ApiResponse(responseCode = "409", description = "Idempotency key reused with a different body")
    public ResponseEntity<PaymentResponse> create(
            @Parameter(description = "Unique client-generated key, 8-128 chars (a UUID works well)", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        CreationResult result = service.createPayment(idempotencyKey, request);
        PaymentResponse body = PaymentResponse.from(result.payment());
        if (result.replayed()) {
            return ResponseEntity.ok().header(REPLAY_HEADER, "true").body(body);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/payments/" + body.id()))
                .header(REPLAY_HEADER, "false")
                .body(body);
    }

    @GetMapping
    @Operation(summary = "Search payments with filters, sorting and pagination (page size capped at 100)")
    public PagedResponse<PaymentResponse> list(
            @Parameter(description = "Matches order reference, customer reference or exact payment id")
            @RequestParam(required = false) String q,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "createdAt | amount | status | orderReference | customerReference")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "asc | desc")
            @RequestParam(defaultValue = "desc") String direction) {

        return PagedResponse.from(
                service.search(q, status, currency, page, size, sort, direction), PaymentResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a payment with its audit trail")
    public PaymentDetailResponse get(@PathVariable UUID id) {
        return service.getDetail(id);
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Fully refund a SUCCEEDED payment",
            description = "Partial refunds are not supported. Declined or already-refunded payments return 409.")
    public PaymentResponse refund(@PathVariable UUID id) {
        return PaymentResponse.from(service.refund(id));
    }
}
