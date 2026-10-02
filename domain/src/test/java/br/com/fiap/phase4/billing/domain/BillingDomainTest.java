package br.com.fiap.phase4.billing.domain;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.model.InvoiceStatus;
import br.com.fiap.phase4.billing.domain.port.out.BillingEventPublisherPort;
import br.com.fiap.phase4.billing.domain.port.out.InvoiceRepositoryPort;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort.MercadoPagoCheckout;
import br.com.fiap.phase4.billing.domain.usecase.CreateInvoiceUseCaseImpl;
import br.com.fiap.phase4.billing.domain.usecase.ProcessMercadoPagoPaymentUseCaseImpl;
import br.com.fiap.phase4.commons.domain.vo.CpfOrCnpj;
import br.com.fiap.phase4.commons.domain.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Billing Domain Tests")
class BillingDomainTest {

    @Mock
    private InvoiceRepositoryPort repository;

    @Mock
    private MercadoPagoGatewayPort mercadoPagoGateway;

    @Mock
    private BillingEventPublisherPort eventPublisher;

    private CreateInvoiceUseCaseImpl createUseCase;
    private ProcessMercadoPagoPaymentUseCaseImpl processPaymentUseCase;

    @BeforeEach
    void setUp() {
        createUseCase = new CreateInvoiceUseCaseImpl(repository);
        processPaymentUseCase = new ProcessMercadoPagoPaymentUseCaseImpl(repository, mercadoPagoGateway, eventPublisher);
    }

    @Nested
    @DisplayName("Invoice Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create invoice when not already present")
        void shouldCreateInvoice() {
            UUID workOrderId = UUID.randomUUID();
            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());
            when(repository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

            Invoice result = createUseCase.create(workOrderId, "52998224725", BigDecimal.valueOf(350.00));

            assertThat(result).isNotNull();
            assertThat(result.getWorkOrderId()).isEqualTo(workOrderId);
            assertThat(result.getAmount().amount()).isEqualByComparingTo("350.00");
            assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PENDING);
            verify(repository).save(any(Invoice.class));
        }

        @Test
        @DisplayName("Should return existing invoice idempotently")
        void shouldReturnExistingIdempotently() {
            UUID workOrderId = UUID.randomUUID();
            Invoice existing = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(350.00));
            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(existing));

            Invoice result = createUseCase.create(workOrderId, "52998224725", BigDecimal.valueOf(350.00));

            assertThat(result).isSameAs(existing);
            verify(repository, never()).save(any(Invoice.class));
        }
    }

    @Nested
    @DisplayName("Mercado Pago Webhook & Saga Confirmation Tests")
    class PaymentWebhookTests {

        @Test
        @DisplayName("Should process Mercado Pago notification, confirm invoice, and publish PaymentConfirmedEvent")
        void shouldConfirmPaymentViaWebhook() {
            String paymentId = "mp-pay-999";
            UUID workOrderId = UUID.randomUUID();
            Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(200.00));
            invoice.setExternalPaymentId(paymentId);

            when(mercadoPagoGateway.verifyPaymentStatus(paymentId)).thenReturn(true);
            when(repository.findByExternalPaymentId(paymentId)).thenReturn(Optional.of(invoice));

            processPaymentUseCase.handleWebhookNotification("payment.created", paymentId, "trace-456");

            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
            verify(repository).save(invoice);
            verify(eventPublisher).publishPaymentConfirmed(invoice, "trace-456");
        }
    }
}
