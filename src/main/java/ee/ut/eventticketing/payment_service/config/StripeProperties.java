package ee.ut.eventticketing.payment_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payments.stripe")
public record StripeProperties(
        String secretKey,
        String publishableKey,
        String webhookSecret) {

    public boolean hasApiKeys() {
        return hasText(secretKey) && hasText(publishableKey);
    }

    public boolean hasWebhookSecret() {
        return hasText(webhookSecret);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
