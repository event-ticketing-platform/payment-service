package ee.ut.eventticketing.payment_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ee.ut.eventticketing.payment_service.dto.CreatePaymentRequest;
import ee.ut.eventticketing.payment_service.dto.PaymentResponse;
import ee.ut.eventticketing.payment_service.dto.StripePaymentIntentResponse;
import ee.ut.eventticketing.payment_service.dto.StripeWebhookResponse;
import ee.ut.eventticketing.payment_service.service.PaymentService;
import jakarta.validation.Valid;

@RestController
@RequestMapping
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public PaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request);
    }

    @GetMapping("/payments/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public PaymentResponse getPayment(@PathVariable Long id) {
        return paymentService.getPayment(id);
    }

    @GetMapping("/payments/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public List<PaymentResponse> getPaymentsByBooking(@PathVariable Long bookingId) {
        return paymentService.getPaymentsByBooking(bookingId);
    }

    @GetMapping("/bookings/{bookingId}/payment")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public PaymentResponse getPaymentByBooking(@PathVariable Long bookingId) {
        return paymentService.getPaymentByBooking(bookingId);
    }

    @PostMapping("/payments/{id}/process")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse processPayment(@PathVariable Long id) {
        return paymentService.processPayment(id);
    }

    @PostMapping("/payments/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse refundPayment(@PathVariable Long id) {
        return paymentService.refundPayment(id);
    }

    @PostMapping("/payments/{id}/fail")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse failPayment(@PathVariable Long id) {
        return paymentService.failPayment(id);
    }

    @PostMapping("/payments/{id}/stripe/intent")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public StripePaymentIntentResponse createStripePaymentIntent(@PathVariable Long id) {
        return paymentService.createStripePaymentIntent(id);
    }

    @PostMapping("/payments/{id}/stripe/confirm")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public PaymentResponse confirmStripePayment(@PathVariable Long id) {
        return paymentService.confirmStripePayment(id);
    }

    @PostMapping(value = "/payments/stripe/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public StripeWebhookResponse stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        paymentService.handleStripeWebhook(payload, signature);
        return new StripeWebhookResponse(true);
    }
}
