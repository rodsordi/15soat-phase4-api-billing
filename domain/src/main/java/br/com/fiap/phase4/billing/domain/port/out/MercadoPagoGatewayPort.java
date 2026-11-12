package br.com.fiap.phase4.billing.domain.port.out;

import br.com.fiap.phase4.billing.domain.model.Invoice;

public interface MercadoPagoGatewayPort {

    record MercadoPagoCheckout(
            String paymentId,
            String qrCode,
            String qrCodeBase64,
            String status
    ) {}

    enum PaymentVerificationStatus {
        APPROVED,
        REJECTED,
        PENDING
    }

    record PaymentVerificationResult(
            PaymentVerificationStatus status,
            String externalPaymentId,
            String rejectionReason
    ) {}

    MercadoPagoCheckout createCheckout(Invoice invoice);

    PaymentVerificationResult verifyPaymentStatus(String externalPaymentId);

    void refundPayment(String externalPaymentId);
}
