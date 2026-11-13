package br.com.fiap.phase4.billing.domain.usecase;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.port.in.ProcessMercadoPagoPaymentUseCase;
import br.com.fiap.phase4.billing.domain.port.out.BillingEventPublisherPort;
import br.com.fiap.phase4.billing.domain.port.out.InvoiceRepositoryPort;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort.MercadoPagoCheckout;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort.PaymentVerificationResult;
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
        PaymentVerificationResult verification = mercadoPagoGateway.verifyPaymentStatus(paymentId);
        if (verification == null || verification.status() == null) {
            return;
        }

        String cleanPaymentId = paymentId.replace("-REJECT", "").replace("-FAIL", "");
        var optInvoice = repository.findByExternalPaymentId(paymentId);
        if (optInvoice.isEmpty() && !cleanPaymentId.equals(paymentId)) {
            optInvoice = repository.findByExternalPaymentId(cleanPaymentId);
        }
        if (optInvoice.isEmpty()) {
            try {
                optInvoice = repository.findById(UUID.fromString(cleanPaymentId));
            } catch (Exception ignored) {}
        }

        optInvoice.ifPresent(invoice -> {
            switch (verification.status()) {
                case APPROVED -> {
                    invoice.markAsPaid(paymentId);
                    repository.save(invoice);
                    eventPublisher.publishPaymentConfirmed(invoice, traceId);
                }
                case REJECTED -> {
                    String reason = verification.rejectionReason() != null ? verification.rejectionReason() : "Payment rejected by gateway";
                    invoice.markAsFailed(reason);
                    repository.save(invoice);
                    eventPublisher.publishPaymentFailed(invoice, reason, traceId);
                }
                case PENDING -> {
                    // Payment is still awaiting customer completion; no state transition required
                }
            }
        });
    }

    @Override
    public void processPaymentFailure(UUID workOrderId, String reason, String traceId) {
        repository.findByWorkOrderId(workOrderId).ifPresent(invoice -> {
            invoice.markAsFailed(reason);
            repository.save(invoice);
            eventPublisher.publishPaymentFailed(invoice, reason, traceId);
        });
    }

    @Override
    public void processCompensatingRefund(UUID workOrderId, String reason, String traceId) {
        repository.findByWorkOrderId(workOrderId).ifPresent(invoice -> {
            invoice.refund();
            if (invoice.getExternalPaymentId() != null) {
                mercadoPagoGateway.refundPayment(invoice.getExternalPaymentId());
            }
            repository.save(invoice);
        });
    }
}
