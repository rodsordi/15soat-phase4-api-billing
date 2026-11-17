package br.com.fiap.phase4.billing.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EstimateItem Model Unit Tests")
class EstimateItemTest {

    @Nested
    @DisplayName("Given creation of EstimateItem")
    class GivenCreation {

        @Nested
        @DisplayName("When valid arguments are provided")
        class WhenValidArguments {

            @Test
            @DisplayName("Should create item using static factory")
            void shouldCreateWithFactory() {
                UUID estimateId = UUID.randomUUID();
                EstimateItem item = EstimateItem.of(
                        estimateId,
                        EstimateItemType.SERVICE,
                        "SRV-OIL",
                        "Oil Change",
                        new BigDecimal("120.00"),
                        2
                );

                assertThat(item.getId()).isNotNull();
                assertThat(item.getEstimateId()).isEqualTo(estimateId);
                assertThat(item.getItemType()).isEqualTo(EstimateItemType.SERVICE);
                assertThat(item.getItemCodeOrSku()).isEqualTo("SRV-OIL");
                assertThat(item.getName()).isEqualTo("Oil Change");
                assertThat(item.getUnitPrice()).isEqualByComparingTo("120.00");
                assertThat(item.getQuantity()).isEqualTo(2);
                assertThat(item.getSubtotal()).isEqualByComparingTo("240.00");
                assertThat(item.getCreatedAt()).isNotNull();
            }

            @Test
            @DisplayName("Should default quantity to 1 when null or non-positive")
            void shouldDefaultQuantityToOne() {
                EstimateItem itemNullQty = new EstimateItem(
                        null,
                        null,
                        EstimateItemType.MATERIAL,
                        "MAT-FILTER",
                        "Air Filter",
                        new BigDecimal("45.00"),
                        null,
                        null,
                        null
                );

                assertThat(itemNullQty.getQuantity()).isEqualTo(1);
                assertThat(itemNullQty.getSubtotal()).isEqualByComparingTo("45.00");

                EstimateItem itemZeroQty = new EstimateItem(
                        null,
                        null,
                        EstimateItemType.MATERIAL,
                        "MAT-FILTER",
                        "Air Filter",
                        new BigDecimal("45.00"),
                        0,
                        null,
                        null
                );

                assertThat(itemZeroQty.getQuantity()).isEqualTo(1);
            }

            @Test
            @DisplayName("Should set estimateId successfully")
            void shouldSetEstimateId() {
                EstimateItem item = EstimateItem.of(null, EstimateItemType.SERVICE, "SRV-ALIGN", "Wheel Alignment", new BigDecimal("80.00"), 1);
                UUID newEstimateId = UUID.randomUUID();

                item.setEstimateId(newEstimateId);

                assertThat(item.getEstimateId()).isEqualTo(newEstimateId);
            }
        }

        @Nested
        @DisplayName("When required fields are missing")
        class WhenRequiredFieldsMissing {

            @Test
            @DisplayName("Should throw NullPointerException when itemType is null")
            void shouldThrowWhenItemTypeIsNull() {
                assertThatThrownBy(() -> new EstimateItem(null, null, null, "SKU", "Name", BigDecimal.TEN, 1, null, Instant.now()))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("Item type is required");
            }

            @Test
            @DisplayName("Should throw NullPointerException when itemCodeOrSku is null")
            void shouldThrowWhenItemCodeOrSkuIsNull() {
                assertThatThrownBy(() -> new EstimateItem(null, null, EstimateItemType.MATERIAL, null, "Name", BigDecimal.TEN, 1, null, Instant.now()))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("Item code/SKU is required");
            }

            @Test
            @DisplayName("Should throw NullPointerException when name is null")
            void shouldThrowWhenNameIsNull() {
                assertThatThrownBy(() -> new EstimateItem(null, null, EstimateItemType.MATERIAL, "SKU", null, BigDecimal.TEN, 1, null, Instant.now()))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("Name is required");
            }

            @Test
            @DisplayName("Should throw NullPointerException when unitPrice is null")
            void shouldThrowWhenUnitPriceIsNull() {
                assertThatThrownBy(() -> new EstimateItem(null, null, EstimateItemType.MATERIAL, "SKU", "Name", null, 1, null, Instant.now()))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("Unit price is required");
            }
        }
    }
}
