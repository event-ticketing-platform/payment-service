package ee.ut.eventticketing.payment_service.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ee.ut.eventticketing.payment_service.client.BookingClient;
import ee.ut.eventticketing.payment_service.dto.CreatePaymentRequest;
import ee.ut.eventticketing.payment_service.dto.PaymentResponse;
import ee.ut.eventticketing.payment_service.exception.PaymentNotFoundException;
import ee.ut.eventticketing.payment_service.model.Money;
import ee.ut.eventticketing.payment_service.model.Payment;
import ee.ut.eventticketing.payment_service.repository.PaymentRepository;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingClient bookingClient;

    public PaymentService(PaymentRepository paymentRepository, BookingClient bookingClient) {
        this.paymentRepository = paymentRepository;
        this.bookingClient = bookingClient;
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

    public PaymentResponse completePayment(Long paymentId) {
        Payment payment = findPayment(paymentId);
        payment.complete();
        bookingClient.confirmBooking(payment.getBookingId());
        return toResponse(paymentRepository.save(payment));
    }

    public PaymentResponse failPayment(Long paymentId) {
        Payment payment = findPayment(paymentId);
        payment.fail();
        return toResponse(paymentRepository.save(payment));
    }

    private Payment findPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
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
