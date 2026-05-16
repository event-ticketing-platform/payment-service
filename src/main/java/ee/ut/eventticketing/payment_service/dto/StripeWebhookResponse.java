package ee.ut.eventticketing.payment_service.dto;

public record StripeWebhookResponse(
        boolean received) {
}
