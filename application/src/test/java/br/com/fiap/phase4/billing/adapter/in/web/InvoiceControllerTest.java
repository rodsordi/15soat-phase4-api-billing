package br.com.fiap.phase4.billing.adapter.in.web;

import br.com.fiap.phase4.billing.adapter.in.web.advice.BillingExceptionHandler;
import br.com.fiap.phase4.billing.adapter.in.web.dto.CheckoutRequest;
import br.com.fiap.phase4.billing.adapter.in.web.dto.CreateInvoiceRequest;
import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.model.InvoiceStatus;
import br.com.fiap.phase4.billing.domain.port.in.CreateInvoiceUseCase;
import br.com.fiap.phase4.billing.domain.port.in.GetInvoiceUseCase;
import br.com.fiap.phase4.billing.domain.port.in.ProcessMercadoPagoPaymentUseCase;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort.MercadoPagoCheckout;
import br.com.fiap.phase4.commons.domain.exception.ResourceNotFoundException;
import br.com.fiap.phase4.commons.domain.vo.CpfOrCnpj;
import br.com.fiap.phase4.commons.domain.vo.Money;
import br.com.fiap.phase4.commons.web.advice.CommonsExceptionAdvice;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Invoice Controller Web Unit Tests")
class InvoiceControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private CreateInvoiceUseCase createInvoiceUseCase;

    @Mock
    private GetInvoiceUseCase getInvoiceUseCase;

    @Mock
    private ProcessMercadoPagoPaymentUseCase processMercadoPagoPaymentUseCase;

    @InjectMocks
    private InvoiceController invoiceController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(invoiceController)
                .setControllerAdvice(new CommonsExceptionAdvice(), new BillingExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/invoices Tests")
    class CreateInvoiceTests {

        @Test
        @DisplayName("Should return 201 Created with Location header when creating invoice")
        void shouldCreateInvoiceSuccessfully() throws Exception {
            UUID invoiceId = UUID.randomUUID();
            UUID workOrderId = UUID.randomUUID();
            CreateInvoiceRequest request = new CreateInvoiceRequest();
            request.setWorkOrderId(workOrderId);
            request.setCustomerDocument("52998224725");
            request.setAmount(350.00);

            Invoice invoice = new Invoice(
                    invoiceId,
                    workOrderId,
                    new CpfOrCnpj("52998224725"),
                    Money.of(350.00),
                    InvoiceStatus.PENDING,
                    null,
                    Instant.now(),
                    Instant.now(),
                    0L
            );

            when(createInvoiceUseCase.create(eq(workOrderId), eq("52998224725"), any(BigDecimal.class)))
                    .thenReturn(invoice);

            mockMvc.perform(post("/api/v1/invoices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/invoices/" + invoiceId))
                    .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                    .andExpect(jsonPath("$.workOrderId").value(workOrderId.toString()))
                    .andExpect(jsonPath("$.amount").value(350.00))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$._links.self.href").value("/api/v1/invoices/" + invoiceId))
                    .andExpect(jsonPath("$._links.workOrder.href").value("/api/v1/work-orders/" + workOrderId));

            verify(createInvoiceUseCase).create(eq(workOrderId), eq("52998224725"), any(BigDecimal.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/invoices/{id} Tests")
    class GetInvoiceByIdTests {

        @Test
        @DisplayName("Should return 200 OK when invoice exists")
        void shouldReturnInvoiceWhenExists() throws Exception {
            UUID invoiceId = UUID.randomUUID();
            UUID workOrderId = UUID.randomUUID();
            Invoice invoice = new Invoice(
                    invoiceId,
                    workOrderId,
                    new CpfOrCnpj("52998224725"),
                    Money.of(200.00),
                    InvoiceStatus.PAID,
                    "PAY-12345",
                    Instant.now(),
                    Instant.now(),
                    1L
            );

            when(getInvoiceUseCase.getById(invoiceId)).thenReturn(invoice);

            mockMvc.perform(get("/api/v1/invoices/{id}", invoiceId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                    .andExpect(jsonPath("$.status").value("PAID"))
                    .andExpect(jsonPath("$.externalPaymentId").value("PAY-12345"))
                    .andExpect(jsonPath("$.version").value(1));

            verify(getInvoiceUseCase).getById(invoiceId);
        }

        @Test
        @DisplayName("Should return 404 Not Found when invoice does not exist")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID invoiceId = UUID.randomUUID();
            when(getInvoiceUseCase.getById(invoiceId)).thenThrow(new ResourceNotFoundException("Invoice", invoiceId));

            mockMvc.perform(get("/api/v1/invoices/{id}", invoiceId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/invoices/work-order/{workOrderId} Tests")
    class GetInvoiceByWorkOrderIdTests {

        @Test
        @DisplayName("Should return 200 OK when invoice exists for workOrder")
        void shouldReturnInvoiceForWorkOrder() throws Exception {
            UUID invoiceId = UUID.randomUUID();
            UUID workOrderId = UUID.randomUUID();
            Invoice invoice = new Invoice(
                    invoiceId,
                    workOrderId,
                    new CpfOrCnpj("52998224725"),
                    Money.of(450.00),
                    InvoiceStatus.PENDING,
                    null,
                    Instant.now(),
                    Instant.now(),
                    0L
            );

            when(getInvoiceUseCase.getByWorkOrderId(workOrderId)).thenReturn(invoice);

            mockMvc.perform(get("/api/v1/invoices/work-order/{workOrderId}", workOrderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                    .andExpect(jsonPath("$.workOrderId").value(workOrderId.toString()));

            verify(getInvoiceUseCase).getByWorkOrderId(workOrderId);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payments/mercadopago/checkout Tests")
    class CheckoutTests {

        @Test
        @DisplayName("Should return 200 OK with QR Code details when checkout is generated")
        void shouldGenerateCheckoutSuccessfully() throws Exception {
            UUID invoiceId = UUID.randomUUID();
            CheckoutRequest request = new CheckoutRequest();
            request.setInvoiceId(invoiceId);

            MercadoPagoCheckout checkout = new MercadoPagoCheckout(
                    "PAY-987",
                    "00020126580014br.gov.bcb.pix...",
                    "MDAwMjAxMjY1ODAwMTRici5nb3YuYmNiLnBpeC4uLg==",
                    "created"
            );

            when(processMercadoPagoPaymentUseCase.generateCheckout(invoiceId)).thenReturn(checkout);

            mockMvc.perform(post("/api/v1/payments/mercadopago/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.invoiceId").value(invoiceId.toString()))
                    .andExpect(jsonPath("$.paymentId").value("PAY-987"))
                    .andExpect(jsonPath("$.qrCode").value("00020126580014br.gov.bcb.pix..."))
                    .andExpect(jsonPath("$.qrCodeBase64").value("MDAwMjAxMjY1ODAwMTRici5nb3YuYmNiLnBpeC4uLg=="))
                    .andExpect(jsonPath("$.status").value("created"));

            verify(processMercadoPagoPaymentUseCase).generateCheckout(invoiceId);
        }
    }
}
