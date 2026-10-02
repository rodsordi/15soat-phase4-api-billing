package br.com.fiap.phase4.billing.domain.port.in;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort.MercadoPagoCheckout;

import java.util.UUID;

public interface ProcessMercadoPagoPaymentUseCase {

    MercadoPagoCheckout generateCheckout(UUID invoiceId);

    void handleWebhookNotification(String action, String paymentId, String traceId);

    void processPaymentFailure(UUID workOrderId, String reason, String traceId);
}
