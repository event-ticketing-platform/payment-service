package ee.ut.eventticketing.payment_service.exception;

public class StripePaymentException extends RuntimeException {

    public StripePaymentException(String message) {
        super(message);
    }

    public StripePaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
