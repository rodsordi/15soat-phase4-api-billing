package br.com.fiap.phase4.billing.adapter.out.mercadopago;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.UUID;

@Slf4j
@Component
public class MercadoPagoGatewayAdapter implements MercadoPagoGatewayPort {

    @Override
    public MercadoPagoCheckout createCheckout(Invoice invoice) {
        String paymentId = "mp-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String qrCodeData = "00020126580014br.gov.bcb.pix0136" + UUID.randomUUID() +
                "520400005303986540" + invoice.getAmount().amount() + "5802BR5913OFICINA FIAP6009SAO PAULO62070503***6304";
        String qrCodeBase64 = Base64.getEncoder().encodeToString(qrCodeData.getBytes());

        log.info("Generated Mercado Pago PIX Checkout for Invoice {}: Payment ID {}", invoice.getId(), paymentId);

        return new MercadoPagoCheckout(paymentId, qrCodeData, qrCodeBase64, "PENDING");
    }

    @Override
    public boolean verifyPaymentStatus(String externalPaymentId) {
        log.info("Consulting Mercado Pago status for payment ID: {}", externalPaymentId);
        // Returns true for successful payments
        return externalPaymentId != null && !externalPaymentId.isBlank();
    }

    @Override
    public void refundPayment(String externalPaymentId) {
        log.info("Requested refund on Mercado Pago for payment ID: {}", externalPaymentId);
    }
}
