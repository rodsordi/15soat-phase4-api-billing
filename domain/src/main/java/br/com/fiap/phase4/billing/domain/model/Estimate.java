package br.com.fiap.phase4.billing.domain.model;

import br.com.fiap.phase4.commons.domain.exception.DomainException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "estimate")
public class Estimate {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "work_order_id", nullable = false, unique = true)
    private UUID workOrderId;

    @Column(name = "customer_document", nullable = false, length = 14)
    private String customerDocument;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EstimateStatus status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "estimate_id")
    private List<EstimateItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public Estimate(UUID id, UUID workOrderId, String customerDocument, String customerEmail,
                    BigDecimal totalAmount, EstimateStatus status, String rejectionReason,
                    List<EstimateItem> items, Instant createdAt, Instant updatedAt, Long version) {
        this.id = id != null ? id : UUID.randomUUID();
        this.workOrderId = Objects.requireNonNull(workOrderId, "Work Order ID is required");
        this.customerDocument = Objects.requireNonNull(customerDocument, "Customer document is required");
        this.customerEmail = customerEmail;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.status = status != null ? status : EstimateStatus.PENDING_APPROVAL;
        this.rejectionReason = rejectionReason;
        if (items != null) {
            this.items = new ArrayList<>(items);
            this.items.forEach(item -> item.setEstimateId(this.id));
        }
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.version = version;
    }

    public static Estimate create(UUID workOrderId, String customerDocument, String customerEmail,
                                  List<EstimateItem> items) {
        UUID estimateId = UUID.randomUUID();
        BigDecimal total = BigDecimal.ZERO;
        List<EstimateItem> preparedItems = new ArrayList<>();
        if (items != null) {
            for (EstimateItem item : items) {
                item.setEstimateId(estimateId);
                total = total.add(item.getSubtotal());
                preparedItems.add(item);
            }
        }
        return new Estimate(estimateId, workOrderId, customerDocument, customerEmail, total,
                EstimateStatus.PENDING_APPROVAL, null, preparedItems, Instant.now(), Instant.now(), null);
    }

    public void approve() {
        if (this.status == EstimateStatus.APPROVED) {
            return; // Idempotent
        }
        if (this.status == EstimateStatus.REJECTED) {
            throw new DomainException("INVALID_ESTIMATE_STATE", "Cannot approve an already rejected estimate");
        }
        this.status = EstimateStatus.APPROVED;
        this.updatedAt = Instant.now();
    }

    public void reject(String reason) {
        if (this.status == EstimateStatus.REJECTED) {
            return; // Idempotent
        }
        if (this.status == EstimateStatus.APPROVED) {
            throw new DomainException("INVALID_ESTIMATE_STATE", "Cannot reject an already approved estimate");
        }
        this.status = EstimateStatus.REJECTED;
        this.rejectionReason = reason;
        this.updatedAt = Instant.now();
    }
}
