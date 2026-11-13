package br.com.fiap.phase4.billing.adapter.in.kafka;

import br.com.fiap.phase4.billing.domain.port.in.CreateInvoiceUseCase;
import br.com.fiap.phase4.billing.domain.port.in.ProcessMercadoPagoPaymentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingEventsConsumer {

    private final CreateInvoiceUseCase createInvoiceUseCase;
    private final ProcessMercadoPagoPaymentUseCase processMercadoPagoPaymentUseCase;

    @KafkaListener(
            topics = "work-order-events",
            groupId = "${spring.kafka.consumer.group-id:billing-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeWorkOrderApproved(ConsumerRecord<String, Map<String, Object>> record) {
        log.info("Received message on topic work-order-events, key: {}", record.key());
        String eventType = extractEventType(record.value());
        if (eventType != null && !"WorkOrderApprovedEvent".equals(eventType)) {
            log.debug("Ignoring event of type '{}' on work-order-events topic", eventType);
            return;
        }

        Map<String, Object> payload = extractPayload(record.value());
        if (payload == null) {
            return;
        }

        try {
            Object workOrderIdObj = payload.get("workOrderId");
            Object docObj = payload.get("customerDocument");
            Object amountObj = payload.get("totalAmount");

            if (workOrderIdObj == null || docObj == null || amountObj == null) {
                log.warn("Ignoring invalid WorkOrderApprovedEvent payload: {}", payload);
                return;
            }

            UUID workOrderId = UUID.fromString(workOrderIdObj.toString());
            String customerDocument = docObj.toString();
            BigDecimal amount = new BigDecimal(amountObj.toString());

            log.info("Processing WorkOrderApprovedEvent for workOrderId: {}, customer: {}, amount: {}",
                    workOrderId, customerDocument, amount);
            createInvoiceUseCase.create(workOrderId, customerDocument, amount);
        } catch (Exception ex) {
            log.error("Error processing WorkOrderApprovedEvent message: {}", record.value(), ex);
        }
    }

    @KafkaListener(
            topics = "execution-events",
            groupId = "${spring.kafka.consumer.group-id:billing-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeExecutionFailed(
            ConsumerRecord<String, Map<String, Object>> record,
            @Header(name = "X-Trace-Id", required = false) byte[] traceIdBytes
    ) {
        log.info("Received message on topic execution-events, key: {}", record.key());
        String eventType = extractEventType(record.value());
        if (eventType != null && !"ExecutionFailedEvent".equals(eventType)) {
            log.debug("Ignoring event of type '{}' on execution-events topic", eventType);
            return;
        }

        Map<String, Object> payload = extractPayload(record.value());
        if (payload == null) {
            return;
        }

        try {
            Object workOrderIdObj = payload.get("workOrderId");
            Object reasonObj = payload.get("reason");

            if (workOrderIdObj == null) {
                log.warn("Ignoring invalid ExecutionFailedEvent payload: {}", payload);
                return;
            }

            UUID workOrderId = UUID.fromString(workOrderIdObj.toString());
            String reason = reasonObj != null ? reasonObj.toString() : "Execution failed";
            String traceId = traceIdBytes != null ? new String(traceIdBytes, StandardCharsets.UTF_8) : UUID.randomUUID().toString();

            log.info("Processing ExecutionFailedEvent compensating refund for workOrderId: {}, reason: {}",
                    workOrderId, reason);
            processMercadoPagoPaymentUseCase.processCompensatingRefund(workOrderId, reason, traceId);
        } catch (Exception ex) {
            log.error("Error processing ExecutionFailedEvent message: {}", record.value(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPayload(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        if (body.get("payload") instanceof Map<?, ?> inner) {
            return (Map<String, Object>) inner;
        }
        return body;
    }

    private String extractEventType(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        if (body.get("metadata") instanceof Map<?, ?> meta) {
            Object type = meta.get("eventType");
            if (type != null) {
                return type.toString();
            }
        }
        Object type = body.get("eventType");
        return type != null ? type.toString() : null;
    }
}
