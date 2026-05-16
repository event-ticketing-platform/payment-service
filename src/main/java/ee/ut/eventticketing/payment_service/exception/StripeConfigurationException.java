package ee.ut.eventticketing.payment_service.exception;

public class StripeConfigurationException extends RuntimeException {

    public StripeConfigurationException() {
        super("Stripe is not configured. Set STRIPE_SECRET_KEY and STRIPE_PUBLISHABLE_KEY.");
    }

    public StripeConfigurationException(String message) {
        super(message);
    }
}
