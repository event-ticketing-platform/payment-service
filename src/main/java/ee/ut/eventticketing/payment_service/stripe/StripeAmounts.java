package ee.ut.eventticketing.payment_service.stripe;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

public final class StripeAmounts {

    private static final Set<String> ZERO_DECIMAL_CURRENCIES = Set.of(
            "BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW", "MGA",
            "PYG", "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF");

    private StripeAmounts() {
    }

    public static long toMinorUnits(BigDecimal amount, String currency) {
        int scale = ZERO_DECIMAL_CURRENCIES.contains(currency.toUpperCase()) ? 0 : 2;
        return amount
                .movePointRight(scale)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}
