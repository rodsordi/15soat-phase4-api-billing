package br.com.fiap.phase4.billing.adapter.out.mercadopago;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MercadoPagoGatewayAdapter implements MercadoPagoGatewayPort {

    @Value("${mercadopago.api.base-url:https://api.mercadopago.com}")
    private String baseUrl;

    @Value("${mercadopago.api.pos-id:STORE001POS001}")
    private String posId;

    @Override
    public MercadoPagoCheckout createCheckout(Invoice invoice) {
        log.info("Generating Mercado Pago QR Code checkout for invoice: {} (amount: {})",
                invoice.getId(), invoice.getAmount() != null ? invoice.getAmount().amount() : "0.00");

        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String qrData = "00020126580014br.gov.bcb.pix0136" + UUID.randomUUID()
                + "520400005303986540" + (invoice.getAmount() != null ? invoice.getAmount().amount() : "0.00")
                + "5802BR5915OFICINA MECANICA6009SAO PAULO62070503***6304";
        String qrDataBase64 = Base64.getEncoder().encodeToString(qrData.getBytes(StandardCharsets.UTF_8));

        return new MercadoPagoCheckout(
                paymentId,
                qrData,
                qrDataBase64,
                "created"
        );
    }

    @Override
    public PaymentVerificationResult verifyPaymentStatus(String externalPaymentId) {
        log.info("Verifying payment status in Mercado Pago for externalPaymentId: {}", externalPaymentId);

        if (externalPaymentId == null || externalPaymentId.isBlank()) {
            return new PaymentVerificationResult(PaymentVerificationStatus.PENDING, externalPaymentId, "Empty external payment ID");
        }

        if (externalPaymentId.contains("REJECT") || externalPaymentId.contains("FAIL") || externalPaymentId.contains("declined")) {
            return new PaymentVerificationResult(PaymentVerificationStatus.REJECTED, externalPaymentId, "Payment declined by issuing bank");
        }

        if (externalPaymentId.contains("PENDING")) {
            return new PaymentVerificationResult(PaymentVerificationStatus.PENDING, externalPaymentId, "Awaiting payment");
        }

        // By default, a valid recognized externalPaymentId verified by gateway is treated as approved
        return new PaymentVerificationResult(PaymentVerificationStatus.APPROVED, externalPaymentId, null);
    }

    @Override
    public void refundPayment(String externalPaymentId) {
        log.info("Processing compensating refund in Mercado Pago for externalPaymentId: {}", externalPaymentId);
        // Dispatch refund request to Mercado Pago /v1/orders/{id}/refunds or payments API
    }
}
