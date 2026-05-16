package ee.ut.eventticketing.payment_service.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    private String providerReference;

    private String type;

    private LocalDateTime timestamp;

    protected PaymentTransaction() {
    }

    public PaymentTransaction(String providerReference, String type) {
        this.providerReference = providerReference;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    void assignToPayment(Payment payment) {
        this.payment = payment;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public String getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
