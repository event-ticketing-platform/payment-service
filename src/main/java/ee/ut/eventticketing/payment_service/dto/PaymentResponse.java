package ee.ut.eventticketing.payment_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import ee.ut.eventticketing.payment_service.model.PaymentStatus;

public record PaymentResponse(
        Long paymentId,
        Long bookingId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        PaymentStatus status,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {
}
