package br.com.fiap.phase4.billing.domain.model;

import br.com.fiap.phase4.commons.domain.exception.DomainException;
import br.com.fiap.phase4.commons.domain.vo.CpfOrCnpj;
import br.com.fiap.phase4.commons.domain.vo.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "work_order_id", nullable = false, unique = true)
    private UUID workOrderId;

    @Getter(AccessLevel.NONE)
    @Column(name = "customer_document", nullable = false, length = 14)
    private String customerDocument;

    @Getter(AccessLevel.NONE)
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InvoiceStatus status;

    @Column(name = "external_payment_id", length = 100)
    private String externalPaymentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public Invoice(UUID id, UUID workOrderId, CpfOrCnpj customerDocument, Money amount,
                   InvoiceStatus status, String externalPaymentId, Instant createdAt, Instant updatedAt, Long version) {
        this.id = id != null ? id : UUID.randomUUID();
        this.workOrderId = Objects.requireNonNull(workOrderId, "Work Order ID is required");
        this.customerDocument = Objects.requireNonNull(customerDocument, "Customer document is required").value();
        this.amount = Objects.requireNonNull(amount, "Amount is required").amount();
        this.status = status != null ? status : InvoiceStatus.PENDING;
        this.externalPaymentId = externalPaymentId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.version = version;
    }

    public Invoice(UUID id, UUID workOrderId, String customerDocument, BigDecimal amount,
                   InvoiceStatus status, String externalPaymentId, Instant createdAt, Instant updatedAt, Long version) {
        this.id = id != null ? id : UUID.randomUUID();
        this.workOrderId = Objects.requireNonNull(workOrderId, "Work Order ID is required");
        this.customerDocument = Objects.requireNonNull(customerDocument, "Customer document is required");
        this.amount = Objects.requireNonNull(amount, "Amount is required");
        this.status = status != null ? status : InvoiceStatus.PENDING;
        this.externalPaymentId = externalPaymentId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.version = version;
    }

    public Invoice(UUID id, UUID workOrderId, CpfOrCnpj customerDocument, Money amount,
                   InvoiceStatus status, String externalPaymentId, Instant createdAt, Instant updatedAt) {
        this(id, workOrderId, customerDocument, amount, status, externalPaymentId, createdAt, updatedAt, null);
    }

    public static Invoice create(UUID workOrderId, CpfOrCnpj customerDocument, Money amount) {
        Objects.requireNonNull(workOrderId, "Work Order ID is required");
        Objects.requireNonNull(customerDocument, "Customer document is required");
        Objects.requireNonNull(amount, "Amount is required");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Invoice amount must be strictly positive");
        }
        return new Invoice(UUID.randomUUID(), workOrderId, customerDocument, amount,
                InvoiceStatus.PENDING, null, Instant.now(), Instant.now(), null);
    }

    public CpfOrCnpj getCustomerDocument() {
        return this.customerDocument != null ? new CpfOrCnpj(this.customerDocument) : null;
    }

    public Money getAmount() {
        return this.amount != null ? Money.of(this.amount) : null;
    }

    public String getRawCustomerDocument() {
        return this.customerDocument;
    }

    public BigDecimal getRawAmount() {
        return this.amount;
    }

    public void markAsPaid(String externalPaymentId) {
        if (this.status == InvoiceStatus.PAID) {
            return; // Idempotent
        }
        if (this.status == InvoiceStatus.CANCELED || this.status == InvoiceStatus.FAILED || this.status == InvoiceStatus.REFUNDED) {
            throw new DomainException("INVALID_INVOICE_STATE", "Cannot pay an invoice in status: " + this.status);
        }
        this.status = InvoiceStatus.PAID;
        this.externalPaymentId = externalPaymentId;
        this.updatedAt = Instant.now();
    }

    public void markAsFailed(String reason) {
        if (this.status == InvoiceStatus.FAILED) {
            return; // Idempotent
        }
        if (this.status == InvoiceStatus.PAID || this.status == InvoiceStatus.REFUNDED) {
            throw new DomainException("INVALID_INVOICE_STATE", "Cannot fail an already paid or refunded invoice");
        }
        this.status = InvoiceStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (this.status == InvoiceStatus.CANCELED) {
            return; // Idempotent
        }
        if (this.status == InvoiceStatus.PAID) {
            throw new DomainException("INVALID_INVOICE_STATE", "Cannot cancel an already paid invoice (must refund)");
        }
        this.status = InvoiceStatus.CANCELED;
        this.updatedAt = Instant.now();
    }

    public void refund() {
        if (this.status == InvoiceStatus.REFUNDED) {
            return; // Idempotent
        }
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
