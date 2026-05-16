package ee.ut.eventticketing.payment_service.stripe;

public record StripeRefund(
        String id,
        String status) {
}
