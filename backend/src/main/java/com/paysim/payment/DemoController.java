
package com.paysim.payment;

import com.paysim.dashboard.DashboardService;
import com.paysim.dashboard.DashboardSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo")
@Tag(name = "Public Demo", description = "Read-only public demo data")
public class DemoController {

    private final PaymentService payments;
    private final DashboardService dashboard;

    public DemoController(PaymentService payments, DashboardService dashboard) {
        this.payments = payments;
        this.dashboard = dashboard;
    }

    @GetMapping("/dashboard/summary")
    @Operation(summary = "Get public demo dashboard metrics")
    public DashboardSummary summary() {
        return dashboard.demoSummary();
    }

    @GetMapping("/payments")
    @Operation(summary = "Search public demo payments")
    public PagedResponse<PaymentResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        Page<Payment> result = payments.searchDemo(
                q, status, currency, page, size, sort, direction);

        return PagedResponse.from(result, PaymentResponse::from);
    }

    @GetMapping("/payments/{id}")
    @Operation(summary = "Get public demo payment details and audit history")
    public PaymentDetailResponse get(@PathVariable UUID id) {
        return payments.getDemoDetail(id);
    }
}
