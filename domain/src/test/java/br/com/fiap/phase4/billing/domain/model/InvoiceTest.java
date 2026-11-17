package br.com.fiap.phase4.billing.domain.model;

import br.com.fiap.phase4.commons.domain.exception.DomainException;
import br.com.fiap.phase4.commons.domain.vo.CpfOrCnpj;
import br.com.fiap.phase4.commons.domain.vo.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Invoice Domain Model Unit Tests")
class InvoiceTest {

    private final CpfOrCnpj validCpf = new CpfOrCnpj("52998224725");
    private final Money validAmount = Money.of(new BigDecimal("250.00"));

    @Nested
    @DisplayName("Given invoice factory creation")
    class GivenInvoiceCreation {

        @Nested
        @DisplayName("When valid parameters are provided")
        class WhenValidParameters {

            @Test
            @DisplayName("Should successfully instantiate pending invoice")
            void shouldCreatePendingInvoice() {
                UUID workOrderId = UUID.randomUUID();
                Invoice invoice = Invoice.create(workOrderId, validCpf, validAmount);

                assertThat(invoice.getId()).isNotNull();
                assertThat(invoice.getWorkOrderId()).isEqualTo(workOrderId);
                assertThat(invoice.getCustomerDocument()).isEqualTo(validCpf);
                assertThat(invoice.getRawCustomerDocument()).isEqualTo("52998224725");
                assertThat(invoice.getAmount()).isEqualTo(validAmount);
                assertThat(invoice.getRawAmount()).isEqualByComparingTo("250.00");
                assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
                assertThat(invoice.getExternalPaymentId()).isNull();
                assertThat(invoice.getCreatedAt()).isNotNull();
                assertThat(invoice.getUpdatedAt()).isNotNull();
                assertThat(invoice.getVersion()).isNull();
            }
        }

        @Nested
        @DisplayName("When invalid arguments are supplied")
        class WhenInvalidArguments {

            @Test
            @DisplayName("Should throw NullPointerException when workOrderId is null")
            void shouldThrowWhenWorkOrderIdIsNull() {
                assertThatThrownBy(() -> Invoice.create(null, validCpf, validAmount))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("Work Order ID is required");
            }

            @Test
            @DisplayName("Should throw NullPointerException when customerDocument is null")
            void shouldThrowWhenCustomerDocumentIsNull() {
                UUID workOrderId = UUID.randomUUID();
                assertThatThrownBy(() -> Invoice.create(workOrderId, null, validAmount))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("Customer document is required");
            }

            @Test
            @DisplayName("Should throw NullPointerException when amount is null")
            void shouldThrowWhenAmountIsNull() {
                UUID workOrderId = UUID.randomUUID();
                assertThatThrownBy(() -> Invoice.create(workOrderId, validCpf, null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("Amount is required");
            }

            @ParameterizedTest
            @CsvSource({"0.00", "-10.00", "-0.01"})
            @DisplayName("Should throw IllegalArgumentException when amount is not positive")
            void shouldThrowWhenAmountIsNotPositive(String invalidAmountValue) {
                UUID workOrderId = UUID.randomUUID();
                Money nonPositiveAmount = Money.of(new BigDecimal(invalidAmountValue));

                assertThatThrownBy(() -> Invoice.create(workOrderId, validCpf, nonPositiveAmount))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Invoice amount must be strictly positive");
            }
        }

        @Nested
        @DisplayName("When using custom constructors")
        class WhenUsingCustomConstructors {

            @Test
            @DisplayName("Should reconstruct with String and BigDecimal constructor")
            void shouldReconstructWithStringAndBigDecimal() {
                UUID id = UUID.randomUUID();
                UUID workOrderId = UUID.randomUUID();
                Instant now = Instant.now();

                Invoice invoice = new Invoice(id, workOrderId, "52998224725", new BigDecimal("123.45"),
                        InvoiceStatus.PAID, "ext-123", now, now, 1L);

                assertThat(invoice.getId()).isEqualTo(id);
                assertThat(invoice.getWorkOrderId()).isEqualTo(workOrderId);
                assertThat(invoice.getRawCustomerDocument()).isEqualTo("52998224725");
                assertThat(invoice.getRawAmount()).isEqualByComparingTo("123.45");
                assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
                assertThat(invoice.getExternalPaymentId()).isEqualTo("ext-123");
                assertThat(invoice.getVersion()).isEqualTo(1L);
            }

            @Test
            @DisplayName("Should reconstruct with 8-parameter constructor")
            void shouldReconstructWith8Parameters() {
                UUID id = UUID.randomUUID();
                UUID workOrderId = UUID.randomUUID();
                Instant now = Instant.now();

                Invoice invoice = new Invoice(id, workOrderId, validCpf, validAmount,
                        InvoiceStatus.PENDING, null, now, now);

                assertThat(invoice.getId()).isEqualTo(id);
                assertThat(invoice.getVersion()).isNull();
            }
        }
    }

    @Nested
    @DisplayName("Given invoice payment state transitions")
    class GivenPaymentTransitions {

        @Nested
        @DisplayName("When marking as paid")
        class WhenMarkingAsPaid {

            @Test
            @DisplayName("Should successfully mark pending invoice as paid")
            void shouldMarkAsPaid() {
                Invoice invoice = Invoice.create(UUID.randomUUID(), validCpf, validAmount);

                invoice.markAsPaid("PAY-SUCCESS-001");

                assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
                assertThat(invoice.getExternalPaymentId()).isEqualTo("PAY-SUCCESS-001");
            }

            @Test
            @DisplayName("Should be idempotent when already in PAID status")
            void shouldBeIdempotentWhenAlreadyPaid() {
                Invoice invoice = Invoice.create(UUID.randomUUID(), validCpf, validAmount);
                invoice.markAsPaid("PAY-FIRST");

                invoice.markAsPaid("PAY-SECOND");

                assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
                assertThat(invoice.getExternalPaymentId()).isEqualTo("PAY-FIRST");
            }

            @Test
            @DisplayName("Should throw DomainException when trying to pay failed, canceled, or refunded invoice")
            void shouldThrowWhenPayingTerminalState() {
                Invoice failedInvoice = Invoice.create(UUID.randomUUID(), validCpf, validAmount);
                failedInvoice.markAsFailed("Reason");
                assertThatThrownBy(() -> failedInvoice.markAsPaid("PAY-INVALID"))
                        .isInstanceOf(DomainException.class)
                        .hasMessageContaining("Cannot pay an invoice in status: FAILED");

                Invoice canceledInvoice = Invoice.create(UUID.randomUUID(), validCpf, validAmount);
                canceledInvoice.cancel("Reason");
                assertThatThrownBy(() -> canceledInvoice.markAsPaid("PAY-INVALID"))
                        .isInstanceOf(DomainException.class)
                        .hasMessageContaining("Cannot pay an invoice in status: CANCELED");

                Invoice refundedInvoice = Invoice.create(UUID.randomUUID(), validCpf, validAmount);
                refundedInvoice.markAsPaid("PAY-OK");
                refundedInvoice.refund();
                assertThatThrownBy(() -> refundedInvoice.markAsPaid("PAY-INVALID"))
                        .isInstanceOf(DomainException.class)
                        .hasMessageContaining("Cannot pay an invoice in status: REFUNDED");
            }
        }

        @Nested
        @DisplayName("When marking as failed")
        class WhenMarkingAsFailed {

            @Test
            @DisplayName("Should mark pending invoice as failed")
            void shouldMarkAsFailed() {
                Invoice invoice = Invoice.create(UUID.randomUUID(), validCpf, validAmount);

                invoice.markAsFailed("Gateway rejection");

                assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.FAILED);
            }

            @Test
            @DisplayName("Should be idempotent when already failed")
            void shouldBeIdempotentWhenAlreadyFailed() {
                Invoice invoice = Invoice.create(UUID.randomUUID(), validCpf, validAmount);
                invoice.markAsFailed("Initial failure");

                invoice.markAsFailed("Secondary failure");

                assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.FAILED);
            }

            @Test
            @DisplayName("Should throw DomainException when trying to fail paid or refunded invoice")
            void shouldThrowWhenFailingPaidOrRefunded() {
                Invoice paidInvoice = Invoice.create(UUID.randomUUID(), validCpf, validAmount);
                paidInvoice.markAsPaid("PAY-OK");
                assertThatThrownBy(() -> paidInvoice.markAsFailed("Fail"))
                        .isInstanceOf(DomainException.class)
                        .hasMessageContaining("Cannot fail an already paid or refunded invoice");

                Invoice refundedInvoice = Invoice.create(UUID.randomUUID(), validCpf, validAmount);
                refundedInvoice.markAsPaid("PAY-OK");
                refundedInvoice.refund();
                assertThatThrownBy(() -> refundedInvoice.markAsFailed("Fail"))
                        .isInstanceOf(DomainException.class)
                        .hasMessageContaining("Cannot fail an already paid or refunded invoice");
            }
        }

        @Nested
        @DisplayName("When canceling an invoice")
        class WhenCancelingInvoice {

            @Test
            @DisplayName("Should cancel pending invoice")
            void shouldCancelPendingInvoice() {
                Invoice invoice = Invoice.create(UUID.randomUUID(), validCpf, validAmount);

                invoice.cancel("Customer request");

                assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELED);
            }

            @Test
            @DisplayName("Should be idempotent when already canceled")
            void shouldBeIdempotentWhenAlreadyCanceled() {
                Invoice invoice = Invoice.create(UUID.randomUUID(), validCpf, validAmount);
                invoice.cancel("Customer request");

                invoice.cancel("Duplicate cancellation");

                assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELED);
            }

            @Test
            @DisplayName("Should throw DomainException when canceling a paid invoice")
            void shouldThrowWhenCancelingPaidInvoice() {
                Invoice invoice = Invoice.create(UUID.randomUUID(), validCpf, validAmount);
                invoice.markAsPaid("PAY-OK");

                assertThatThrownBy(() -> invoice.cancel("Attempt cancel"))
                        .isInstanceOf(DomainException.class)
                        .hasMessageContaining("Cannot cancel an already paid invoice (must refund)");
            }
        }

        @Nested
        @DisplayName("When refunding an invoice")
        class WhenRefundingInvoice {

            @Test
            @DisplayName("Should refund a paid invoice")
            void shouldRefundPaidInvoice() {
                Invoice invoice = Invoice.create(UUID.randomUUID(), validCpf, validAmount);
                invoice.markAsPaid("PAY-OK");

                invoice.refund();

                assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.REFUNDED);
            }

            @Test
            @DisplayName("Should be idempotent when already refunded")
            void shouldBeIdempotentWhenAlreadyRefunded() {
                Invoice invoice = Invoice.create(UUID.randomUUID(), validCpf, validAmount);
                invoice.markAsPaid("PAY-OK");
                invoice.refund();

                invoice.refund();

                assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.REFUNDED);
            }

            @Test
            @DisplayName("Should throw DomainException when refunding non-paid invoice")
            void shouldThrowWhenRefundingNonPaidInvoice() {
                Invoice invoice = Invoice.create(UUID.randomUUID(), validCpf, validAmount);

                assertThatThrownBy(invoice::refund)
                        .isInstanceOf(DomainException.class)
                        .hasMessageContaining("Only paid invoices can be refunded");
            }
        }

        @Nested
        @DisplayName("When updating external payment ID")
        class WhenUpdatingExternalPaymentId {

            @Test
            @DisplayName("Should update externalPaymentId and updatedAt timestamp")
            void shouldUpdateExternalPaymentId() {
                Invoice invoice = Invoice.create(UUID.randomUUID(), validCpf, validAmount);

                invoice.setExternalPaymentId("NEW-EXT-ID");

                assertThat(invoice.getExternalPaymentId()).isEqualTo("NEW-EXT-ID");
            }
        }
    }
}
