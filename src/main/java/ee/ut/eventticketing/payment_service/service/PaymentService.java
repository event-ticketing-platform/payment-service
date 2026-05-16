package ee.ut.eventticketing.payment_service.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;

import ee.ut.eventticketing.payment_service.dto.CreatePaymentRequest;
import ee.ut.eventticketing.payment_service.dto.PaymentResponse;
import ee.ut.eventticketing.payment_service.dto.StripePaymentIntentResponse;
import ee.ut.eventticketing.payment_service.exception.PaymentNotFoundException;
import ee.ut.eventticketing.payment_service.exception.StripePaymentException;
import ee.ut.eventticketing.payment_service.messaging.PaymentEventPublisher;
import ee.ut.eventticketing.payment_service.model.Money;
import ee.ut.eventticketing.payment_service.model.Payment;
import ee.ut.eventticketing.payment_service.model.PaymentStatus;
import ee.ut.eventticketing.payment_service.repository.PaymentRepository;
import ee.ut.eventticketing.payment_service.stripe.StripePaymentGateway;
import ee.ut.eventticketing.payment_service.stripe.StripePaymentIntent;
import ee.ut.eventticketing.payment_service.stripe.StripeRefund;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final StripePaymentGateway stripePaymentGateway;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentEventPublisher paymentEventPublisher,
            StripePaymentGateway stripePaymentGateway) {
        this.paymentRepository = paymentRepository;
        this.paymentEventPublisher = paymentEventPublisher;
        this.stripePaymentGateway = stripePaymentGateway;
    }

    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Payment payment = new Payment(
                request.bookingId(),
                new Money(request.amount(), request.currency()),
                request.paymentMethod());

        return toResponse(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId) {
        return toResponse(findPayment(paymentId));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBooking(Long bookingId) {
        return paymentRepository.findFirstByBookingIdOrderByPaymentIdDesc(bookingId)
                .map(this::toResponse)
                .orElseThrow(() -> new PaymentNotFoundException("Payment for booking " + bookingId + " was not found"));
    }

    public PaymentResponse processPayment(Long paymentId) {
        Payment payment = findPayment(paymentId);
        if (payment.getStripePaymentIntentId() != null) {
            return confirmStripePayment(paymentId);
        }
        payment.complete();
        Payment saved = paymentRepository.save(payment);
        paymentEventPublisher.publishPaymentCompleted(saved);
        return toResponse(saved);
    }

    public PaymentResponse failPayment(Long paymentId) {
        Payment payment = findPayment(paymentId);
        payment.fail();
        return toResponse(paymentRepository.save(payment));
    }

    public PaymentResponse refundPayment(Long paymentId) {
        Payment payment = findPayment(paymentId);
        if (payment.getStripePaymentIntentId() != null) {
            StripeRefund refund = stripePaymentGateway.refundPaymentIntent(
                    payment.getStripePaymentIntentId(),
                    payment.getAmount().getAmount(),
                    payment.getAmount().getCurrency());
            payment.refundWithProvider(refund.id());
        } else {
            payment.refund();
        }
        return toResponse(paymentRepository.save(payment));
    }

    public StripePaymentIntentResponse createStripePaymentIntent(Long paymentId) {
        Payment payment = findPayment(paymentId);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can start Stripe checkout");
        }

        if (payment.getStripePaymentIntentId() != null && payment.getStripeClientSecret() != null) {
            StripePaymentIntent intent = stripePaymentGateway.retrievePaymentIntent(payment.getStripePaymentIntentId());
            return toStripeResponse(payment, intent.status());
        }

        StripePaymentIntent intent = stripePaymentGateway.createPaymentIntent(payment);
        payment.attachStripePaymentIntent(intent.id(), intent.clientSecret());
        Payment saved = paymentRepository.save(payment);
        return toStripeResponse(saved, intent.status());
    }

    public PaymentResponse confirmStripePayment(Long paymentId) {
        Payment payment = findPayment(paymentId);
        if (payment.getStripePaymentIntentId() == null) {
            throw new IllegalStateException("Payment " + paymentId + " has no Stripe payment intent");
        }

        StripePaymentIntent intent = stripePaymentGateway.retrievePaymentIntent(payment.getStripePaymentIntentId());
        if ("succeeded".equals(intent.status())) {
            return completeStripePayment(payment, intent.id());
        }
        if ("canceled".equals(intent.status())) {
            payment.fail();
            return toResponse(paymentRepository.save(payment));
        }

        throw new StripePaymentException("Stripe payment is not complete yet. Current status: " + intent.status());
    }

    public void handleStripeWebhook(String payload, String signature) {
        Event event = stripePaymentGateway.constructWebhookEvent(payload, signature);
        if ("payment_intent.succeeded".equals(event.getType())) {
            handleStripeSucceeded(extractPaymentIntent(event));
        } else if ("payment_intent.payment_failed".equals(event.getType())
                || "payment_intent.canceled".equals(event.getType())) {
            handleStripeFailed(extractPaymentIntent(event));
        }
    }

    private Payment findPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    private PaymentResponse completeStripePayment(Payment payment, String paymentIntentId) {
        boolean wasAlreadyCompleted = payment.getStatus() == PaymentStatus.COMPLETED;
        payment.completeWithProvider(paymentIntentId);
        Payment saved = paymentRepository.save(payment);
        if (!wasAlreadyCompleted) {
            paymentEventPublisher.publishPaymentCompleted(saved);
        }
        return toResponse(saved);
    }

    private void handleStripeSucceeded(PaymentIntent intent) {
        Payment payment = findPaymentForIntent(intent);
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return;
        }
        completeStripePayment(payment, intent.getId());
    }

    private void handleStripeFailed(PaymentIntent intent) {
        Payment payment = findPaymentForIntent(intent);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }
        payment.fail();
        paymentRepository.save(payment);
    }

    private Payment findPaymentForIntent(PaymentIntent intent) {
        return paymentRepository.findByStripePaymentIntentId(intent.getId())
                .or(() -> paymentIdFromMetadata(intent).flatMap(paymentRepository::findById))
                .orElseThrow(() -> new PaymentNotFoundException("Payment for Stripe intent " + intent.getId() + " was not found"));
    }

    private java.util.Optional<Long> paymentIdFromMetadata(PaymentIntent intent) {
        try {
            String paymentId = intent.getMetadata().get("paymentId");
            return paymentId == null ? java.util.Optional.empty() : java.util.Optional.of(Long.valueOf(paymentId));
        } catch (NumberFormatException exception) {
            return java.util.Optional.empty();
        }
    }

    private PaymentIntent extractPaymentIntent(Event event) {
        StripeObject object = event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new StripePaymentException("Stripe webhook did not include a payment intent"));
        if (object instanceof PaymentIntent paymentIntent) {
            return paymentIntent;
        }
        throw new StripePaymentException("Stripe webhook payload was not a payment intent");
    }

    private StripePaymentIntentResponse toStripeResponse(Payment payment, String stripeStatus) {
        return new StripePaymentIntentResponse(
                payment.getPaymentId(),
                payment.getStripePaymentIntentId(),
                payment.getStripeClientSecret(),
                stripePaymentGateway.publishableKey(),
                payment.getAmount().getAmount(),
                payment.getAmount().getCurrency(),
                stripeStatus);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getBookingId(),
                payment.getAmount().getAmount(),
                payment.getAmount().getCurrency(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getCompletedAt());
    }
}
