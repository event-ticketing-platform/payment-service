package ee.ut.eventticketing.payment_service.dto;

import java.math.BigDecimal;

public record StripePaymentIntentResponse(
        Long paymentId,
        String paymentIntentId,
        String clientSecret,
        String publishableKey,
        BigDecimal amount,
        String currency,
        String stripeStatus) {
}
