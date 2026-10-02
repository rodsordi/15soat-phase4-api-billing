package br.com.fiap.phase4.billing.adapter.in.web;

import br.com.fiap.phase4.billing.adapter.in.web.api.PaymentWebhookApi;
import br.com.fiap.phase4.billing.adapter.in.web.dto.MercadoPagoWebhookRequest;
import br.com.fiap.phase4.billing.adapter.in.web.dto.WebhookResponse;
import br.com.fiap.phase4.billing.domain.port.in.ProcessMercadoPagoPaymentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentWebhookController implements PaymentWebhookApi {

    private final ProcessMercadoPagoPaymentUseCase processPaymentUseCase;

    @Override
    public ResponseEntity<WebhookResponse> processPaymentWebhook(MercadoPagoWebhookRequest request) {
        String traceId = MDC.get("traceId");
        String paymentId = request.getData() != null ? request.getData().getId() : null;

        log.info("Processing Mercado Pago webhook for action: {}, paymentId: {}", request.getAction(), paymentId);
        if (paymentId != null && !paymentId.isBlank()) {
            processPaymentUseCase.handleWebhookNotification(request.getAction(), paymentId, traceId);
        }

        WebhookResponse response = new WebhookResponse();
        response.setStatus("PROCESSED");
        response.setMessage("Webhook received and processed");

        return ResponseEntity.ok(response);
    }
}
