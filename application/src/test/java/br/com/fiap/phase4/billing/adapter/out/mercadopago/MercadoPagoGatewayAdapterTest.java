package br.com.fiap.phase4.billing.adapter.out.mercadopago;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort.MercadoPagoCheckout;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort.PaymentVerificationResult;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort.PaymentVerificationStatus;
import br.com.fiap.phase4.commons.domain.vo.CpfOrCnpj;
import br.com.fiap.phase4.commons.domain.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Mercado Pago Gateway Adapter Unit Tests")
class MercadoPagoGatewayAdapterTest {

    private MercadoPagoGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MercadoPagoGatewayAdapter();
    }

    @Test
    @DisplayName("Should create checkout with QR Code and paymentId")
    void shouldCreateCheckout() {
        Invoice invoice = Invoice.create(UUID.randomUUID(), new CpfOrCnpj("52998224725"), Money.of(350.00));

        MercadoPagoCheckout checkout = adapter.createCheckout(invoice);

        assertThat(checkout).isNotNull();
        assertThat(checkout.paymentId()).startsWith("PAY-");
        assertThat(checkout.qrCode()).contains("br.gov.bcb.pix");
        assertThat(checkout.qrCodeBase64()).isNotBlank();
        assertThat(checkout.status()).isEqualTo("created");
    }

    @Test
    @DisplayName("Should return APPROVED when verifying valid payment")
    void shouldReturnApprovedForValidPayment() {
        PaymentVerificationResult result = adapter.verifyPaymentStatus("PAY-12345");

        assertThat(result.status()).isEqualTo(PaymentVerificationStatus.APPROVED);
        assertThat(result.externalPaymentId()).isEqualTo("PAY-12345");
    }

    @Test
    @DisplayName("Should return REJECTED when payment contains decline indicator")
    void shouldReturnRejectedForDeclinedPayment() {
        PaymentVerificationResult result = adapter.verifyPaymentStatus("PAY-REJECT-999");

        assertThat(result.status()).isEqualTo(PaymentVerificationStatus.REJECTED);
        assertThat(result.rejectionReason()).contains("Payment declined");
    }

    @Test
    @DisplayName("Should return PENDING when payment contains pending indicator or is blank")
    void shouldReturnPending() {
        PaymentVerificationResult result = adapter.verifyPaymentStatus("PAY-PENDING-001");
        assertThat(result.status()).isEqualTo(PaymentVerificationStatus.PENDING);

        PaymentVerificationResult blankResult = adapter.verifyPaymentStatus("");
        assertThat(blankResult.status()).isEqualTo(PaymentVerificationStatus.PENDING);
    }
}
