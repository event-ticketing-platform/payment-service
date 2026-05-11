package ee.ut.eventticketing.payment_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ee.ut.eventticketing.payment_service.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBookingId(Long bookingId);
}
