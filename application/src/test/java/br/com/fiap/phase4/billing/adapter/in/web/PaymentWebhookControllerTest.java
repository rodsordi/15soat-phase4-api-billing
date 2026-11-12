package br.com.fiap.phase4.billing.adapter.in.web;

import br.com.fiap.phase4.billing.adapter.in.web.advice.BillingExceptionHandler;
import br.com.fiap.phase4.billing.adapter.in.web.dto.MercadoPagoWebhookRequest;
import br.com.fiap.phase4.billing.adapter.in.web.dto.MercadoPagoWebhookRequestData;
import br.com.fiap.phase4.billing.domain.port.in.ProcessMercadoPagoPaymentUseCase;
import br.com.fiap.phase4.commons.web.advice.CommonsExceptionAdvice;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Webhook Controller Unit Tests")
class PaymentWebhookControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProcessMercadoPagoPaymentUseCase processMercadoPagoPaymentUseCase;

    @InjectMocks
    private PaymentWebhookController webhookController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(webhookController)
                .setControllerAdvice(new CommonsExceptionAdvice(), new BillingExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Should process payment webhook notification and return 200 OK")
    void shouldProcessPaymentWebhookSuccessfully() throws Exception {
        MercadoPagoWebhookRequest request = new MercadoPagoWebhookRequest();
        request.setAction("payment.updated");
        MercadoPagoWebhookRequestData data = new MercadoPagoWebhookRequestData();
        data.setId("123456789");
        request.setData(data);

        mockMvc.perform(post("/api/v1/payments/mercadopago/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.message").value("Webhook processed successfully"));

        verify(processMercadoPagoPaymentUseCase).handleWebhookNotification(eq("payment.updated"), eq("123456789"), anyString());
    }
}
