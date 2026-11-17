package br.com.fiap.phase4.billing.domain.port;

import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort.MercadoPagoCheckout;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort.PaymentVerificationResult;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort.PaymentVerificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Mercado Pago Port DTO and Enum Unit Tests")
class MercadoPagoPortTest {

    @Nested
    @DisplayName("Given MercadoPagoCheckout Record")
    class GivenCheckoutRecord {

        @Test
        @DisplayName("Should correctly hold checkout data")
        void shouldHoldCheckoutData() {
            MercadoPagoCheckout checkout = new MercadoPagoCheckout("pay-123", "00020126...", "iVBORw0KGgo...", "PENDING");

            assertThat(checkout.paymentId()).isEqualTo("pay-123");
            assertThat(checkout.qrCode()).isEqualTo("00020126...");
            assertThat(checkout.qrCodeBase64()).isEqualTo("iVBORw0KGgo...");
            assertThat(checkout.status()).isEqualTo("PENDING");
        }
    }

    @Nested
    @DisplayName("Given PaymentVerificationResult Record")
    class GivenVerificationResult {

        @Test
        @DisplayName("Should correctly hold verification status and reason")
        void shouldHoldVerificationData() {
            PaymentVerificationResult result = new PaymentVerificationResult(
                    PaymentVerificationStatus.REJECTED,
                    "pay-456",
                    "insufficient_amount"
            );

            assertThat(result.status()).isEqualTo(PaymentVerificationStatus.REJECTED);
            assertThat(result.externalPaymentId()).isEqualTo("pay-456");
            assertThat(result.rejectionReason()).isEqualTo("insufficient_amount");
        }
    }

    @Nested
    @DisplayName("Given PaymentVerificationStatus Enum")
    class GivenStatusEnum {

        @Test
        @DisplayName("Should contain all expected gateway states")
        void shouldContainAllStates() {
            assertThat(PaymentVerificationStatus.values())
                    .containsExactly(
                            PaymentVerificationStatus.APPROVED,
                            PaymentVerificationStatus.REJECTED,
                            PaymentVerificationStatus.PENDING
                    );
        }
    }
}
