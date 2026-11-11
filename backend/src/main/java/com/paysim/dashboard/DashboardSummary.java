package com.paysim.dashboard;

import com.paysim.audit.RecentActivity;
import com.paysim.payment.PaymentStatus;
import java.util.List;
import java.util.Map;

public record DashboardSummary(
        long totalTransactions,
        long successfulTransactions,
        long declinedTransactions,
        long refundedTransactions,
        Map<PaymentStatus, Long> statusDistribution,
        List<RecentActivity> recentActivity) {
}
