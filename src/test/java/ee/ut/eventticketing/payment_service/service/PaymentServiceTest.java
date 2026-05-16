package ee.ut.eventticketing.payment_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import ee.ut.eventticketing.payment_service.dto.PaymentResponse;
import ee.ut.eventticketing.payment_service.messaging.PaymentEventPublisher;
import ee.ut.eventticketing.payment_service.model.Money;
import ee.ut.eventticketing.payment_service.model.Payment;
import ee.ut.eventticketing.payment_service.model.PaymentStatus;
import ee.ut.eventticketing.payment_service.repository.PaymentRepository;
import ee.ut.eventticketing.payment_service.stripe.StripePaymentGateway;
import ee.ut.eventticketing.payment_service.stripe.StripePaymentIntent;
import ee.ut.eventticketing.payment_service.stripe.StripeRefund;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @Mock
    private StripePaymentGateway stripePaymentGateway;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void processPaymentCompletesPaymentAndPublishesEvent() {
        Payment payment = payment();

        when(paymentRepository.findById(900L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.processPayment(900L);

        assertThat(response.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getTransactions()).hasSize(1);
        verify(paymentEventPublisher).publishPaymentCompleted(payment);
    }

    @Test
    void refundPaymentMarksCompletedPaymentAsRefunded() {
        Payment payment = payment();
        payment.complete();

        when(paymentRepository.findById(900L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.refundPayment(900L);

        assertThat(response.status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getTransactions()).hasSize(2);
    }

    @Test
    void createStripePaymentIntentStoresProviderReferences() {
        Payment payment = payment();
        StripePaymentIntent intent = new StripePaymentIntent(
                "pi_test_123",
                "pi_test_secret_123",
                "requires_payment_method");

        when(paymentRepository.findById(900L)).thenReturn(Optional.of(payment));
        when(stripePaymentGateway.createPaymentIntent(payment)).thenReturn(intent);
        when(stripePaymentGateway.publishableKey()).thenReturn("pk_test_123");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createStripePaymentIntent(900L);

        assertThat(response.paymentIntentId()).isEqualTo("pi_test_123");
        assertThat(response.clientSecret()).isEqualTo("pi_test_secret_123");
        assertThat(response.publishableKey()).isEqualTo("pk_test_123");
        assertThat(payment.getStripePaymentIntentId()).isEqualTo("pi_test_123");
        assertThat(payment.getTransactions()).hasSize(1);
    }

    @Test
    void confirmStripePaymentCompletesPaymentAndPublishesEvent() {
        Payment payment = payment();
        payment.attachStripePaymentIntent("pi_test_123", "pi_test_secret_123");

        when(paymentRepository.findById(900L)).thenReturn(Optional.of(payment));
        when(stripePaymentGateway.retrievePaymentIntent("pi_test_123"))
                .thenReturn(new StripePaymentIntent("pi_test_123", "pi_test_secret_123", "succeeded"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.confirmStripePayment(900L);

        assertThat(response.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getTransactions()).hasSize(2);
        verify(paymentEventPublisher).publishPaymentCompleted(payment);
    }

    @Test
    void confirmStripePaymentDoesNotRepublishCompletedPayment() {
        Payment payment = payment();
        payment.attachStripePaymentIntent("pi_test_123", "pi_test_secret_123");
        payment.completeWithProvider("pi_test_123");

        when(paymentRepository.findById(900L)).thenReturn(Optional.of(payment));
        when(stripePaymentGateway.retrievePaymentIntent("pi_test_123"))
                .thenReturn(new StripePaymentIntent("pi_test_123", "pi_test_secret_123", "succeeded"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.confirmStripePayment(900L);

        assertThat(response.status()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentEventPublisher, never()).publishPaymentCompleted(any(Payment.class));
    }

    @Test
    void refundPaymentUsesStripeRefundWhenIntentExists() {
        Payment payment = payment();
        payment.attachStripePaymentIntent("pi_test_123", "pi_test_secret_123");
        payment.completeWithProvider("pi_test_123");

        when(paymentRepository.findById(900L)).thenReturn(Optional.of(payment));
        when(stripePaymentGateway.refundPaymentIntent("pi_test_123", BigDecimal.valueOf(50.00), "EUR"))
                .thenReturn(new StripeRefund("re_test_123", "succeeded"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.refundPayment(900L);

        assertThat(response.status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getTransactions()).hasSize(3);
    }

    private Payment payment() {
        Payment payment = new Payment(
                100L,
                new Money(BigDecimal.valueOf(50.00), "EUR"),
                "CARD");
        ReflectionTestUtils.setField(payment, "paymentId", 900L);
        return payment;
    }
}
