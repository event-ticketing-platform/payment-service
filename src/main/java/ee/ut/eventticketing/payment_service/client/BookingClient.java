package ee.ut.eventticketing.payment_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BookingClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String bookingServiceBaseUrl;
    private final boolean mockEnabled;

    public BookingClient(
            @Value("${services.booking.base-url}") String bookingServiceBaseUrl,
            @Value("${services.booking.mock-enabled:true}") boolean mockEnabled) {
        this.bookingServiceBaseUrl = bookingServiceBaseUrl;
        this.mockEnabled = mockEnabled;
    }

    public void confirmBooking(Long bookingId) {
        if (mockEnabled) {
            return;
        }

        restTemplate.postForEntity(
                bookingServiceBaseUrl + "/bookings/{id}/confirm",
                null,
                Void.class,
                bookingId);
    }
}
