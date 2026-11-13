package br.com.fiap.phase4.billing.adapter.out.kafka;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.port.out.BillingEventPublisherPort;
import br.com.fiap.phase4.commons.kafka.event.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingEventPublisherAdapter implements BillingEventPublisherPort {

    public static final String PAYMENT_EVENTS_TOPIC = "payment-events";
    public static final String SOURCE_SERVICE = "15soat-phase4-api-billing";
    public static final String TRACE_HEADER = "X-Trace-Id";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishPaymentConfirmed(Invoice invoice, String traceId) {
        log.info("Publishing PaymentConfirmedEvent for invoice: {}, workOrderId: {}, traceId: {}",
                invoice.getId(), invoice.getWorkOrderId(), traceId);

        Map<String, Object> payload = Map.of(
                "invoiceId", invoice.getId(),
                "workOrderId", invoice.getWorkOrderId(),
                "amount", invoice.getAmount() != null ? invoice.getAmount().amount().doubleValue() : 0.0
        );

        var envelope = EventEnvelope.of(
                "PaymentConfirmedEvent",
                SOURCE_SERVICE,
                traceId,
                payload
        );

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                PAYMENT_EVENTS_TOPIC,
                invoice.getWorkOrderId().toString(),
                envelope
        );

        if (traceId != null && !traceId.isBlank()) {
            record.headers().add(new RecordHeader(TRACE_HEADER, traceId.getBytes(StandardCharsets.UTF_8)));
        }

        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish PaymentConfirmedEvent for workOrderId: {}", invoice.getWorkOrderId(), ex);
            } else {
                log.info("Successfully published PaymentConfirmedEvent to topic {} with offset: {}",
                        PAYMENT_EVENTS_TOPIC, result.getRecordMetadata().offset());
            }
        });
    }

    @Override
    public void publishPaymentFailed(Invoice invoice, String reason, String traceId) {
        log.info("Publishing PaymentFailedEvent for invoice: {}, workOrderId: {}, reason: {}, traceId: {}",
                invoice.getId(), invoice.getWorkOrderId(), reason, traceId);

        Map<String, Object> payload = Map.of(
                "workOrderId", invoice.getWorkOrderId(),
                "reason", reason != null ? reason : "UNKNOWN"
        );

        var envelope = EventEnvelope.of(
                "PaymentFailedEvent",
                SOURCE_SERVICE,
                traceId,
                payload
        );

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                PAYMENT_EVENTS_TOPIC,
                invoice.getWorkOrderId().toString(),
                envelope
        );

        if (traceId != null && !traceId.isBlank()) {
            record.headers().add(new RecordHeader(TRACE_HEADER, traceId.getBytes(StandardCharsets.UTF_8)));
        }

        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish PaymentFailedEvent for workOrderId: {}", invoice.getWorkOrderId(), ex);
            } else {
                log.info("Successfully published PaymentFailedEvent to topic {} with offset: {}",
                        PAYMENT_EVENTS_TOPIC, result.getRecordMetadata().offset());
            }
        });
    }
}
