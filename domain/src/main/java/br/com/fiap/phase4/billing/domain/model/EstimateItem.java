package br.com.fiap.phase4.billing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "estimate_item")
public class EstimateItem {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "estimate_id", nullable = false)
    private UUID estimateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private EstimateItemType itemType;

    @Column(name = "item_code_or_sku", nullable = false, length = 50)
    private String itemCodeOrSku;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public EstimateItem(UUID id, UUID estimateId, EstimateItemType itemType, String itemCodeOrSku,
                        String name, BigDecimal unitPrice, Integer quantity, BigDecimal subtotal, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.estimateId = estimateId;
        this.itemType = Objects.requireNonNull(itemType, "Item type is required");
        this.itemCodeOrSku = Objects.requireNonNull(itemCodeOrSku, "Item code/SKU is required");
        this.name = Objects.requireNonNull(name, "Name is required");
        this.unitPrice = Objects.requireNonNull(unitPrice, "Unit price is required");
        this.quantity = quantity != null && quantity > 0 ? quantity : 1;
        this.subtotal = subtotal != null ? subtotal : this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static EstimateItem of(UUID estimateId, EstimateItemType itemType, String itemCodeOrSku,
                                  String name, BigDecimal unitPrice, Integer quantity) {
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity != null ? quantity : 1));
        return new EstimateItem(UUID.randomUUID(), estimateId, itemType, itemCodeOrSku, name, unitPrice, quantity, subtotal, Instant.now());
    }

    public void setEstimateId(UUID estimateId) {
        this.estimateId = estimateId;
    }
}
