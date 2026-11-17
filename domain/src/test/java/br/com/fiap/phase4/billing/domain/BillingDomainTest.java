package br.com.fiap.phase4.billing.domain;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.model.InvoiceStatus;
import br.com.fiap.phase4.billing.domain.port.out.BillingEventPublisherPort;
import br.com.fiap.phase4.billing.domain.port.out.InvoiceRepositoryPort;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort.MercadoPagoCheckout;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort.PaymentVerificationResult;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort.PaymentVerificationStatus;
import br.com.fiap.phase4.billing.domain.usecase.CreateInvoiceUseCaseImpl;
import br.com.fiap.phase4.billing.domain.usecase.ProcessMercadoPagoPaymentUseCaseImpl;
import br.com.fiap.phase4.commons.domain.exception.ResourceNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
            assertThat(result.getVersion()).isNull();
            verify(repository).save(any(Invoice.class));
        }

        @Test
        @DisplayName("Should maintain optimistic lock version when reconstructed")
        void shouldMaintainOptimisticLockVersion() {
            UUID workOrderId = UUID.randomUUID();
            Invoice invoice = new Invoice(UUID.randomUUID(), workOrderId, new CpfOrCnpj("52998224725"),
                    Money.of(150.00), InvoiceStatus.PENDING, null, null, null, 3L);

            assertThat(invoice.getVersion()).isEqualTo(3L);
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
    @DisplayName("Mercado Pago Checkout Generation Tests")
    class CheckoutGenerationTests {

        @Test
        @DisplayName("Should generate checkout and persist external payment ID")
        void shouldGenerateCheckoutSuccessfully() {
            UUID invoiceId = UUID.randomUUID();
            Invoice invoice = Invoice.create(UUID.randomUUID(), new CpfOrCnpj("52998224725"), Money.of(250.00));
            MercadoPagoCheckout checkout = new MercadoPagoCheckout("mp-12345", "qr-payload", "qr-base64", "PENDING");

            when(repository.findById(invoiceId)).thenReturn(Optional.of(invoice));
            when(mercadoPagoGateway.createCheckout(invoice)).thenReturn(checkout);

            MercadoPagoCheckout result = processPaymentUseCase.generateCheckout(invoiceId);

            assertThat(result).isEqualTo(checkout);
            assertThat(invoice.getExternalPaymentId()).isEqualTo("mp-12345");
            verify(repository).save(invoice);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when invoice not found")
        void shouldThrowWhenInvoiceNotFoundForCheckout() {
            UUID invoiceId = UUID.randomUUID();
            when(repository.findById(invoiceId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> processPaymentUseCase.generateCheckout(invoiceId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Invoice")
                    .hasMessageContaining(invoiceId.toString());
        }
    }

    @Nested
    @DisplayName("Mercado Pago Webhook & Saga Confirmation Tests")
    class PaymentWebhookTests {

        @Test
        @DisplayName("Should process approved notification, confirm invoice, and publish PaymentConfirmedEvent")
        void shouldConfirmPaymentViaWebhook() {
            String paymentId = "mp-pay-999";
            UUID workOrderId = UUID.randomUUID();
            Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(200.00));
            invoice.setExternalPaymentId(paymentId);

            when(mercadoPagoGateway.verifyPaymentStatus(paymentId))
                    .thenReturn(new PaymentVerificationResult(PaymentVerificationStatus.APPROVED, paymentId, null));
            when(repository.findByExternalPaymentId(paymentId)).thenReturn(Optional.of(invoice));

            processPaymentUseCase.handleWebhookNotification("payment.created", paymentId, "trace-456");

            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
            verify(repository).save(invoice);
            verify(eventPublisher).publishPaymentConfirmed(invoice, "trace-456");
        }

        @Test
        @DisplayName("Should process rejected notification, fail invoice, and publish PaymentFailedEvent")
        void shouldFailPaymentViaWebhookWhenRejected() {
            String paymentId = "mp-pay-declined";
            UUID workOrderId = UUID.randomUUID();
            Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(300.00));
            invoice.setExternalPaymentId(paymentId);

            when(mercadoPagoGateway.verifyPaymentStatus(paymentId))
                    .thenReturn(new PaymentVerificationResult(PaymentVerificationStatus.REJECTED, paymentId, "Insufficient funds"));
            when(repository.findByExternalPaymentId(paymentId)).thenReturn(Optional.of(invoice));

            processPaymentUseCase.handleWebhookNotification("payment.updated", paymentId, "trace-declined");

            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.FAILED);
            verify(repository).save(invoice);
            verify(eventPublisher).publishPaymentFailed(invoice, "Insufficient funds", "trace-declined");
        }

        @Test
        @DisplayName("Should use default reason when rejectionReason is null")
        void shouldUseDefaultReasonWhenRejectionReasonIsNull() {
            String paymentId = "mp-pay-rejected-no-reason";
            Invoice invoice = Invoice.create(UUID.randomUUID(), new CpfOrCnpj("52998224725"), Money.of(300.00));
            invoice.setExternalPaymentId(paymentId);

            when(mercadoPagoGateway.verifyPaymentStatus(paymentId))
                    .thenReturn(new PaymentVerificationResult(PaymentVerificationStatus.REJECTED, paymentId, null));
            when(repository.findByExternalPaymentId(paymentId)).thenReturn(Optional.of(invoice));

            processPaymentUseCase.handleWebhookNotification("payment.updated", paymentId, "trace-null-reason");

            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.FAILED);
            verify(eventPublisher).publishPaymentFailed(invoice, "Payment rejected by gateway", "trace-null-reason");
        }

        @Test
        @DisplayName("Should leave invoice pending when payment verification returns PENDING")
        void shouldLeaveInvoicePendingWhenVerificationIsPending() {
            String paymentId = "mp-pay-pending";
            UUID workOrderId = UUID.randomUUID();
            Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(300.00));
            invoice.setExternalPaymentId(paymentId);

            when(mercadoPagoGateway.verifyPaymentStatus(paymentId))
                    .thenReturn(new PaymentVerificationResult(PaymentVerificationStatus.PENDING, paymentId, null));
            when(repository.findByExternalPaymentId(paymentId)).thenReturn(Optional.of(invoice));

            processPaymentUseCase.handleWebhookNotification("payment.updated", paymentId, "trace-pending");

            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
            verify(repository, never()).save(invoice);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Should return early without processing when verification is null")
        void shouldReturnEarlyWhenVerificationIsNull() {
            when(mercadoPagoGateway.verifyPaymentStatus("null-verif")).thenReturn(null);

            processPaymentUseCase.handleWebhookNotification("action", "null-verif", "trace");

            verifyNoInteractions(repository);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Should return early without processing when verification status is null")
        void shouldReturnEarlyWhenVerificationStatusIsNull() {
            when(mercadoPagoGateway.verifyPaymentStatus("null-status"))
                    .thenReturn(new PaymentVerificationResult(null, "null-status", null));

            processPaymentUseCase.handleWebhookNotification("action", "null-status", "trace");

            verifyNoInteractions(repository);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Should find invoice by cleaned payment ID when suffix is removed")
        void shouldFindInvoiceByCleanPaymentId() {
            String rawPaymentId = "pay-12345-REJECT";
            String cleanPaymentId = "pay-12345";
            Invoice invoice = Invoice.create(UUID.randomUUID(), new CpfOrCnpj("52998224725"), Money.of(100.00));

            when(mercadoPagoGateway.verifyPaymentStatus(rawPaymentId))
                    .thenReturn(new PaymentVerificationResult(PaymentVerificationStatus.APPROVED, rawPaymentId, null));
            when(repository.findByExternalPaymentId(rawPaymentId)).thenReturn(Optional.empty());
            when(repository.findByExternalPaymentId(cleanPaymentId)).thenReturn(Optional.of(invoice));

            processPaymentUseCase.handleWebhookNotification("action", rawPaymentId, "trace-clean");

            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
            verify(repository).save(invoice);
        }

        @Test
        @DisplayName("Should find invoice by UUID fallback when cleanPaymentId is valid UUID")
        void shouldFindInvoiceByUuidFallback() {
            UUID invoiceId = UUID.randomUUID();
            String rawPaymentId = invoiceId.toString() + "-FAIL";
            Invoice invoice = Invoice.create(UUID.randomUUID(), new CpfOrCnpj("52998224725"), Money.of(100.00));

            when(mercadoPagoGateway.verifyPaymentStatus(rawPaymentId))
                    .thenReturn(new PaymentVerificationResult(PaymentVerificationStatus.APPROVED, rawPaymentId, null));
            when(repository.findByExternalPaymentId(rawPaymentId)).thenReturn(Optional.empty());
            when(repository.findByExternalPaymentId(invoiceId.toString())).thenReturn(Optional.empty());
            when(repository.findById(invoiceId)).thenReturn(Optional.of(invoice));

            processPaymentUseCase.handleWebhookNotification("action", rawPaymentId, "trace-uuid");

            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
            verify(repository).save(invoice);
        }

        @Test
        @DisplayName("Should do nothing when invoice not found in any lookup")
        void shouldDoNothingWhenInvoiceNotFound() {
            String paymentId = "unknown-payment";
            when(mercadoPagoGateway.verifyPaymentStatus(paymentId))
                    .thenReturn(new PaymentVerificationResult(PaymentVerificationStatus.APPROVED, paymentId, null));
            when(repository.findByExternalPaymentId(paymentId)).thenReturn(Optional.empty());

            processPaymentUseCase.handleWebhookNotification("action", paymentId, "trace-notfound");

            verify(repository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("Payment Failure & Saga Compensation Tests")
    class PaymentFailureTests {

        @Test
        @DisplayName("Should mark invoice as FAILED and publish PaymentFailedEvent")
        void shouldProcessPaymentFailure() {
            UUID workOrderId = UUID.randomUUID();
            Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(250.00));
            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(invoice));

            processPaymentUseCase.processPaymentFailure(workOrderId, "EXPIRED_QR_CODE", "trace-789");

            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.FAILED);
            verify(repository).save(invoice);
            verify(eventPublisher).publishPaymentFailed(invoice, "EXPIRED_QR_CODE", "trace-789");
        }

        @Test
        @DisplayName("Should do nothing when invoice not found on processPaymentFailure")
        void shouldDoNothingWhenInvoiceNotFoundOnFailure() {
            UUID workOrderId = UUID.randomUUID();
            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

            processPaymentUseCase.processPaymentFailure(workOrderId, "REASON", "trace");

            verify(repository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Should process compensating refund when execution fails")
        void shouldProcessCompensatingRefund() {
            UUID workOrderId = UUID.randomUUID();
            String paymentId = "mp-pay-refund-123";
            Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(500.00));
            invoice.markAsPaid(paymentId);
            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(invoice));

            processPaymentUseCase.processCompensatingRefund(workOrderId, "Execution canceled", "trace-refund");

            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.REFUNDED);
            verify(mercadoPagoGateway).refundPayment(paymentId);
            verify(repository).save(invoice);
        }

        @Test
        @DisplayName("Should not call gateway refund when externalPaymentId is null")
        void shouldNotCallGatewayRefundWhenExternalPaymentIdIsNull() {
            UUID workOrderId = UUID.randomUUID();
            Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(500.00));
            invoice.markAsPaid("temp-id");
            invoice.setExternalPaymentId(null);
            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(invoice));

            processPaymentUseCase.processCompensatingRefund(workOrderId, "Execution canceled", "trace-refund");

            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.REFUNDED);
            verify(mercadoPagoGateway, never()).refundPayment(any());
            verify(repository).save(invoice);
        }

        @Test
        @DisplayName("Should do nothing when invoice not found on processCompensatingRefund")
        void shouldDoNothingWhenInvoiceNotFoundOnRefund() {
            UUID workOrderId = UUID.randomUUID();
            when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

            processPaymentUseCase.processCompensatingRefund(workOrderId, "REASON", "trace");

            verify(repository, never()).save(any());
            verifyNoInteractions(mercadoPagoGateway);
        }
    }
}
