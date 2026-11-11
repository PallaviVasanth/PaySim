package com.paysim.idempotency;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 over the normalised business fields of a request, used to detect key reuse with a different payload. */
public final class RequestFingerprint {

    private RequestFingerprint() {
    }

    public static String of(String customerReference, String orderReference, BigDecimal amount,
                            String currency, String description) {
        String canonical = String.join("\n",
                customerReference,
                orderReference,
                amount.stripTrailingZeros().toPlainString(),
                currency,
                description == null ? "" : description);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the JVM specification", e);
        }
    }
}
