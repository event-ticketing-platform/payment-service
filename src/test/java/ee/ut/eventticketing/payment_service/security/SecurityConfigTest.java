package ee.ut.eventticketing.payment_service.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import ee.ut.eventticketing.payment_service.controller.PaymentController;
import ee.ut.eventticketing.payment_service.service.PaymentService;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void paymentEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/payments/1"))
                .andExpect(status().isUnauthorized());
    }
}
