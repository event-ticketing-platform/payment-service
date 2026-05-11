package ee.ut.eventticketing.payment_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
        @NotNull Long bookingId,
        @NotNull @DecimalMin("0.00") BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String paymentMethod) {
}
