package br.com.fiap.phase4.billing.adapter.in.web;

import br.com.fiap.phase4.billing.adapter.in.web.api.PaymentWebhookApi;
import br.com.fiap.phase4.billing.adapter.in.web.dto.MercadoPagoWebhookRequest;
import br.com.fiap.phase4.billing.adapter.in.web.dto.WebhookResponse;
import br.com.fiap.phase4.billing.domain.port.in.ProcessMercadoPagoPaymentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentWebhookController implements PaymentWebhookApi {

    private final ProcessMercadoPagoPaymentUseCase processMercadoPagoPaymentUseCase;

    @Override
    public ResponseEntity<WebhookResponse> processPaymentWebhook(MercadoPagoWebhookRequest request) {
        String action = request.getAction();
        String paymentId = request.getData() != null ? request.getData().getId() : null;
        String traceId = UUID.randomUUID().toString();

        log.info("REST: Received Mercado Pago payment webhook notification: action={}, paymentId={}, traceId={}",
                action, paymentId, traceId);

        if (paymentId != null && !paymentId.isBlank()) {
            processMercadoPagoPaymentUseCase.handleWebhookNotification(action, paymentId, traceId);
        } else {
            log.warn("REST: Webhook received without paymentId in data payload");
        }

        WebhookResponse response = new WebhookResponse()
                .status("PROCESSED")
                .message("Webhook processed successfully");

        return ResponseEntity.ok(response);
    }
}
