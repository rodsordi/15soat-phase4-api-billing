package br.com.fiap.phase4.billing.adapter.out.kafka;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.port.out.BillingEventPublisherPort;
import br.com.fiap.phase4.commons.kafka.event.EventEnvelope;
import br.com.fiap.phase4.commons.kafka.tracing.KafkaTraceContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingKafkaPublisherAdapter implements BillingEventPublisherPort {

    public static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishPaymentConfirmed(Invoice invoice, String traceId) {
        Map<String, Object> payload = Map.of(
                "invoiceId", invoice.getId().toString(),
                "workOrderId", invoice.getWorkOrderId().toString(),
                "customerDocument", invoice.getCustomerDocument().value(),
                "amount", invoice.getAmount().amount(),
                "status", invoice.getStatus().name(),
                "externalPaymentId", invoice.getExternalPaymentId() != null ? invoice.getExternalPaymentId() : ""
        );

        var envelope = EventEnvelope.of(
                "PaymentConfirmedEvent",
                "api-billing",
                traceId,
                payload
        );

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                PAYMENT_EVENTS_TOPIC,
                invoice.getWorkOrderId().toString(),
                envelope
        );

        KafkaTraceContextUtils.injectTraceContext(record.headers(), traceId, null);
        kafkaTemplate.send(record);
        log.info("Published PaymentConfirmedEvent for Work Order ID: {} on topic: {}", invoice.getWorkOrderId(), PAYMENT_EVENTS_TOPIC);
    }

    @Override
    public void publishPaymentFailed(Invoice invoice, String reason, String traceId) {
        Map<String, Object> payload = Map.of(
                "invoiceId", invoice.getId().toString(),
                "workOrderId", invoice.getWorkOrderId().toString(),
                "reason", reason != null ? reason : "Payment declined",
                "status", invoice.getStatus().name()
        );

        var envelope = EventEnvelope.of(
                "PaymentFailedEvent",
                "api-billing",
                traceId,
                payload
        );

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                PAYMENT_EVENTS_TOPIC,
                invoice.getWorkOrderId().toString(),
                envelope
        );

        KafkaTraceContextUtils.injectTraceContext(record.headers(), traceId, null);
        kafkaTemplate.send(record);
        log.warn("Published PaymentFailedEvent (Saga Rollback) for Work Order ID: {} on topic: {}", invoice.getWorkOrderId(), PAYMENT_EVENTS_TOPIC);
    }
}
