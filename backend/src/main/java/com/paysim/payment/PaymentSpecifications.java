package com.paysim.payment;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Composable search filters. All user input is bound as parameters (no string concatenation into SQL). */
final class PaymentSpecifications {

    private PaymentSpecifications() {
    }

    static Specification<Payment> matching(String query, PaymentStatus status, String currency) {
        return (root, cq, cb) -> {
            List<Predicate> all = new ArrayList<>();
            if (status != null) {
                all.add(cb.equal(root.get("status"), status));
            }
            if (currency != null && !currency.isBlank()) {
                all.add(cb.equal(root.get("currency"), currency.trim().toUpperCase(Locale.ROOT)));
            }
            if (query != null && !query.isBlank()) {
                String term = query.trim();
                String like = "%" + escapeLike(term.toLowerCase(Locale.ROOT)) + "%";
                List<Predicate> any = new ArrayList<>();
                any.add(cb.like(cb.lower(root.get("orderReference")), like, '\\'));
                any.add(cb.like(cb.lower(root.get("customerReference")), like, '\\'));
                parseUuid(term).ifPresent(id -> any.add(cb.equal(root.get("id"), id)));
                all.add(cb.or(any.toArray(new Predicate[0])));
            }
            return cb.and(all.toArray(new Predicate[0]));
        };
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static Optional<UUID> parseUuid(String s) {
        try {
            return Optional.of(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
