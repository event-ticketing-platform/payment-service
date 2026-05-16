package ee.ut.eventticketing.payment_service.messaging;

import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ee.ut.eventticketing.events.PaymentCompletedEvent;
import ee.ut.eventticketing.payment_service.config.RabbitMessagingConfig;
import ee.ut.eventticketing.payment_service.model.Payment;

@Component
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String paymentEventsExchange;

    public PaymentEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${messaging.exchanges.payment-events}") String paymentEventsExchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.paymentEventsExchange = paymentEventsExchange;
    }

    public void publishPaymentCompleted(Payment payment) {
        rabbitTemplate.convertAndSend(
                paymentEventsExchange,
                RabbitMessagingConfig.PAYMENT_COMPLETED_ROUTING_KEY,
                new PaymentCompletedEvent(
                        payment.getPaymentId(),
                        payment.getBookingId(),
                        payment.getAmount().getAmount(),
                        payment.getAmount().getCurrency(),
                        payment.getStatus().name(),
                        LocalDateTime.now()));
    }
}
