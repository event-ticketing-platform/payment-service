package ee.ut.eventticketing.payment_service.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    private Long bookingId;

    @Embedded
    private Money amount;

    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    private String stripePaymentIntentId;

    private String stripeClientSecret;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentTransaction> transactions = new ArrayList<>();

    protected Payment() {
    }

    public Payment(Long bookingId, Money amount, String paymentMethod) {
        this.bookingId = bookingId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void complete() {
        completeWithProvider("provider-" + paymentId + "-" + System.currentTimeMillis());
    }

    public void completeWithProvider(String providerReference) {
        if (status == PaymentStatus.COMPLETED) {
            return;
        }
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be completed");
        }
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        addTransaction(new PaymentTransaction(providerReference, "PROCESS"));
    }

    public void fail() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be failed");
        }
        this.status = PaymentStatus.FAILED;
    }

    public void refund() {
        refundWithProvider("refund-" + paymentId + "-" + System.currentTimeMillis());
    }

    public void refundWithProvider(String providerReference) {
        if (status == PaymentStatus.REFUNDED) {
            return;
        }
        if (status != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Only completed payments can be refunded");
        }
        this.status = PaymentStatus.REFUNDED;
        addTransaction(new PaymentTransaction(providerReference, "REFUND"));
    }

    public void attachStripePaymentIntent(String paymentIntentId, String clientSecret) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can start Stripe checkout");
        }
        this.stripePaymentIntentId = paymentIntentId;
        this.stripeClientSecret = clientSecret;
        addTransaction(new PaymentTransaction(paymentIntentId, "STRIPE_PAYMENT_INTENT"));
    }

    private void addTransaction(PaymentTransaction transaction) {
        transaction.assignToPayment(this);
        this.transactions.add(transaction);
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public Money getAmount() {
        return amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public String getStripeClientSecret() {
        return stripeClientSecret;
    }

    public List<PaymentTransaction> getTransactions() {
        return transactions;
    }
}
