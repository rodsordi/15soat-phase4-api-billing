package br.com.fiap.phase4.billing.adapter.out.persistence;

import br.com.fiap.phase4.billing.domain.model.InvoiceStatus;
import br.com.fiap.phase4.commons.postgres.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoice")
public class InvoiceJpaEntity extends AuditableEntity {

    @Column(name = "work_order_id", nullable = false, unique = true)
    private UUID workOrderId;

    @Column(name = "customer_document", nullable = false, length = 14)
    private String customerDocument;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InvoiceStatus status;

    @Column(name = "external_payment_id", length = 100)
    private String externalPaymentId;

    public InvoiceJpaEntity(UUID id, UUID workOrderId, String customerDocument,
                            BigDecimal amount, InvoiceStatus status, String externalPaymentId) {
        super(id);
        this.workOrderId = workOrderId;
        this.customerDocument = customerDocument;
        this.amount = amount;
        this.status = status;
        this.externalPaymentId = externalPaymentId;
    }
}
