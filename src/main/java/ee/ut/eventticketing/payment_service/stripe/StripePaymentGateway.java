package ee.ut.eventticketing.payment_service.stripe;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;

import ee.ut.eventticketing.payment_service.config.StripeProperties;
import ee.ut.eventticketing.payment_service.exception.StripeConfigurationException;
import ee.ut.eventticketing.payment_service.exception.StripePaymentException;
import ee.ut.eventticketing.payment_service.model.Payment;

@Component
public class StripePaymentGateway {

    private final StripeProperties properties;

    public StripePaymentGateway(StripeProperties properties) {
        this.properties = properties;
    }

    public String publishableKey() {
        ensureApiKeysConfigured();
        return properties.publishableKey();
    }

    public StripePaymentIntent createPaymentIntent(Payment payment) {
        ensureApiKeysConfigured();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(StripeAmounts.toMinorUnits(
                        payment.getAmount().getAmount(),
                        payment.getAmount().getCurrency()))
                .setCurrency(payment.getAmount().getCurrency().toLowerCase())
                .setDescription("Event ticket booking #" + payment.getBookingId())
                .putMetadata("paymentId", String.valueOf(payment.getPaymentId()))
                .putMetadata("bookingId", String.valueOf(payment.getBookingId()))
                .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build())
                .build();

        try {
            PaymentIntent intent = PaymentIntent.create(params, requestOptions());
            return toPaymentIntent(intent);
        } catch (StripeException exception) {
            throw new StripePaymentException("Stripe could not create a payment intent", exception);
        }
    }

    public StripePaymentIntent retrievePaymentIntent(String paymentIntentId) {
        ensureApiKeysConfigured();
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId, requestOptions());
            return toPaymentIntent(intent);
        } catch (StripeException exception) {
            throw new StripePaymentException("Stripe could not retrieve payment intent " + paymentIntentId, exception);
        }
    }

    public StripeRefund refundPaymentIntent(String paymentIntentId, BigDecimal amount, String currency) {
        ensureApiKeysConfigured();

        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .setAmount(StripeAmounts.toMinorUnits(amount, currency))
                .build();

        try {
            Refund refund = Refund.create(params, requestOptions());
            return new StripeRefund(refund.getId(), refund.getStatus());
        } catch (StripeException exception) {
            throw new StripePaymentException("Stripe could not refund payment intent " + paymentIntentId, exception);
        }
    }

    public Event constructWebhookEvent(String payload, String signature) {
        if (!properties.hasWebhookSecret()) {
            throw new StripeConfigurationException("Stripe webhook secret is not configured. Set STRIPE_WEBHOOK_SECRET.");
        }
        try {
            return Webhook.constructEvent(payload, signature, properties.webhookSecret());
        } catch (Exception exception) {
            throw new StripePaymentException("Stripe webhook signature verification failed", exception);
        }
    }

    private StripePaymentIntent toPaymentIntent(PaymentIntent intent) {
        return new StripePaymentIntent(
                intent.getId(),
                intent.getClientSecret(),
                intent.getStatus());
    }

    private RequestOptions requestOptions() {
        return RequestOptions.builder()
                .setApiKey(properties.secretKey())
                .build();
    }

    private void ensureApiKeysConfigured() {
        if (!properties.hasApiKeys()) {
            throw new StripeConfigurationException();
        }
    }
}
