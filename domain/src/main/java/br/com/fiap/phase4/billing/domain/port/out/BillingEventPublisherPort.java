package br.com.fiap.phase4.billing.domain.port.out;

import br.com.fiap.phase4.billing.domain.model.Invoice;

public interface BillingEventPublisherPort {

    void publishPaymentConfirmed(Invoice invoice, String traceId);

    void publishPaymentFailed(Invoice invoice, String reason, String traceId);
}
