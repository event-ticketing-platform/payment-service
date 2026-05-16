package ee.ut.eventticketing.payment_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import ee.ut.eventticketing.payment_service.dto.CreatePaymentRequest;
import ee.ut.eventticketing.payment_service.dto.PaymentResponse;
import ee.ut.eventticketing.payment_service.dto.StripePaymentIntentResponse;
import ee.ut.eventticketing.payment_service.model.PaymentStatus;
import ee.ut.eventticketing.payment_service.service.PaymentService;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    void createPaymentReturnsCreatedPayment() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(
                100L,
                BigDecimal.valueOf(50.00),
                "EUR",
                "CARD");
        PaymentResponse response = response(PaymentStatus.PENDING);

        when(paymentService.createPayment(any(CreatePaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(900))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void processPaymentReturnsCompletedPayment() throws Exception {
        when(paymentService.processPayment(900L)).thenReturn(response(PaymentStatus.COMPLETED));

        mockMvc.perform(post("/payments/900/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void createStripeIntentReturnsClientSecret() throws Exception {
        StripePaymentIntentResponse response = new StripePaymentIntentResponse(
                900L,
                "pi_test_123",
                "pi_test_secret_123",
                "pk_test_123",
                BigDecimal.valueOf(50.00),
                "EUR",
                "requires_payment_method");

        when(paymentService.createStripePaymentIntent(900L)).thenReturn(response);

        mockMvc.perform(post("/payments/900/stripe/intent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentIntentId").value("pi_test_123"))
                .andExpect(jsonPath("$.clientSecret").value("pi_test_secret_123"))
                .andExpect(jsonPath("$.publishableKey").value("pk_test_123"));
    }

    @Test
    void confirmStripePaymentReturnsCompletedPayment() throws Exception {
        when(paymentService.confirmStripePayment(900L)).thenReturn(response(PaymentStatus.COMPLETED));

        mockMvc.perform(post("/payments/900/stripe/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void createPaymentRejectsInvalidRequest() throws Exception {
        String invalidRequest = """
                {
                  "bookingId": 100,
                  "currency": "EUR",
                  "paymentMethod": "CARD"
                }
                """;

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    private PaymentResponse response(PaymentStatus status) {
        return new PaymentResponse(
                900L,
                100L,
                BigDecimal.valueOf(50.00),
                "EUR",
                "CARD",
                status,
                LocalDateTime.parse("2026-05-03T12:00:00"),
                status == PaymentStatus.COMPLETED ? LocalDateTime.parse("2026-05-03T12:01:00") : null);
    }
}
