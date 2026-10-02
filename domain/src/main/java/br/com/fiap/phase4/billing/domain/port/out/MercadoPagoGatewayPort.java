package br.com.fiap.phase4.billing.domain.port.out;

import br.com.fiap.phase4.billing.domain.model.Invoice;

public interface MercadoPagoGatewayPort {

    record MercadoPagoCheckout(
            String paymentId,
            String qrCode,
            String qrCodeBase64,
            String status
    ) {}

    MercadoPagoCheckout createCheckout(Invoice invoice);

    boolean verifyPaymentStatus(String externalPaymentId);

    void refundPayment(String externalPaymentId);
}
