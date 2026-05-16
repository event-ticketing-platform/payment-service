package ee.ut.eventticketing.payment_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ee.ut.eventticketing.payment_service.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findFirstByBookingIdOrderByPaymentIdDesc(Long bookingId);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
}
