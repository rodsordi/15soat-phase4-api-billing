package br.com.fiap.phase4.billing.domain.usecase;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.port.in.ProcessMercadoPagoPaymentUseCase;
import br.com.fiap.phase4.billing.domain.port.out.BillingEventPublisherPort;
import br.com.fiap.phase4.billing.domain.port.out.InvoiceRepositoryPort;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort.MercadoPagoCheckout;
import br.com.fiap.phase4.commons.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessMercadoPagoPaymentUseCaseImpl implements ProcessMercadoPagoPaymentUseCase {

    private final InvoiceRepositoryPort repository;
    private final MercadoPagoGatewayPort mercadoPagoGateway;
    private final BillingEventPublisherPort eventPublisher;

    @Override
    public MercadoPagoCheckout generateCheckout(UUID invoiceId) {
        Invoice invoice = repository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));

        MercadoPagoCheckout checkout = mercadoPagoGateway.createCheckout(invoice);
        invoice.setExternalPaymentId(checkout.paymentId());
        repository.save(invoice);

        return checkout;
    }

    @Override
    public void handleWebhookNotification(String action, String paymentId, String traceId) {
        boolean isApproved = mercadoPagoGateway.verifyPaymentStatus(paymentId);
        if (!isApproved) {
            return;
        }

        repository.findByExternalPaymentId(paymentId).ifPresent(invoice -> {
            invoice.markAsPaid(paymentId);
            repository.save(invoice);
            eventPublisher.publishPaymentConfirmed(invoice, traceId);
        });
    }

    @Override
    public void processPaymentFailure(UUID workOrderId, String reason, String traceId) {
        repository.findByWorkOrderId(workOrderId).ifPresent(invoice -> {
            invoice.cancel(reason);
            repository.save(invoice);
            eventPublisher.publishPaymentFailed(invoice, reason, traceId);
        });
    }
}
