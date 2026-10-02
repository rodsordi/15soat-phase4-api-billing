package br.com.fiap.phase4.billing.domain.model;

import br.com.fiap.phase4.commons.domain.exception.DomainException;
import br.com.fiap.phase4.commons.domain.vo.CpfOrCnpj;
import br.com.fiap.phase4.commons.domain.vo.Money;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice {

    private UUID id;
    private UUID workOrderId;
    private CpfOrCnpj customerDocument;
    private Money amount;
    private InvoiceStatus status;
    private String externalPaymentId;
    private Instant createdAt;
    private Instant updatedAt;

    public Invoice(UUID id, UUID workOrderId, CpfOrCnpj customerDocument, Money amount,
                   InvoiceStatus status, String externalPaymentId, Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.workOrderId = Objects.requireNonNull(workOrderId, "Work Order ID is required");
        this.customerDocument = Objects.requireNonNull(customerDocument, "Customer document is required");
        this.amount = Objects.requireNonNull(amount, "Amount is required");
        this.status = status != null ? status : InvoiceStatus.PENDING;
        this.externalPaymentId = externalPaymentId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Invoice create(UUID workOrderId, CpfOrCnpj customerDocument, Money amount) {
        return new Invoice(UUID.randomUUID(), workOrderId, customerDocument, amount,
                InvoiceStatus.PENDING, null, Instant.now(), Instant.now());
    }

    public void markAsPaid(String externalPaymentId) {
        if (this.status == InvoiceStatus.PAID) {
            return; // Idempotent
        }
        if (this.status == InvoiceStatus.CANCELED) {
            throw new DomainException("INVALID_INVOICE_STATE", "Cannot pay an already canceled invoice");
        }
        this.status = InvoiceStatus.PAID;
        this.externalPaymentId = externalPaymentId;
        this.updatedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (this.status == InvoiceStatus.PAID) {
            throw new DomainException("INVALID_INVOICE_STATE", "Cannot cancel an already paid invoice (must refund)");
        }
        this.status = InvoiceStatus.CANCELED;
        this.updatedAt = Instant.now();
    }

    public void refund() {
        if (this.status != InvoiceStatus.PAID) {
            throw new DomainException("INVALID_INVOICE_STATE", "Only paid invoices can be refunded");
        }
        this.status = InvoiceStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }

    public void setExternalPaymentId(String externalPaymentId) {
        this.externalPaymentId = externalPaymentId;
        this.updatedAt = Instant.now();
    }
}
