package br.com.fiap.phase4.billing.adapter.out.kafka;

import br.com.fiap.phase4.billing.domain.model.Estimate;
import br.com.fiap.phase4.billing.domain.port.out.EstimateEventPublisherPort;
import br.com.fiap.phase4.commons.kafka.event.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EstimateKafkaEventPublisherAdapter implements EstimateEventPublisherPort {

    public static final String BILLING_EVENTS_TOPIC = "billing-events";
    public static final String SOURCE_SERVICE = "15soat-phase4-api-billing";
    public static final String TRACE_HEADER = "X-Trace-Id";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishEstimateCreated(Estimate estimate, String traceId) {
        log.info("Publishing EstimateCreatedEvent for estimate: {}, workOrderId: {}", estimate.getId(), estimate.getWorkOrderId());
        Map<String, Object> payload = Map.of(
                "estimateId", estimate.getId(),
                "workOrderId", estimate.getWorkOrderId(),
                "customerDocument", estimate.getCustomerDocument(),
                "totalAmount", estimate.getTotalAmount().doubleValue(),
                "status", estimate.getStatus().name()
        );

        send(estimate, "EstimateCreatedEvent", payload, traceId);
    }

    @Override
    public void publishEstimateApproved(Estimate estimate, String traceId) {
        log.info("Publishing EstimateApprovedEvent for estimate: {}, workOrderId: {}", estimate.getId(), estimate.getWorkOrderId());
        Map<String, Object> payload = Map.of(
                "estimateId", estimate.getId(),
                "workOrderId", estimate.getWorkOrderId(),
                "customerDocument", estimate.getCustomerDocument(),
                "totalAmount", estimate.getTotalAmount().doubleValue(),
                "status", estimate.getStatus().name()
        );

        send(estimate, "EstimateApprovedEvent", payload, traceId);
    }

    @Override
    public void publishEstimateRejected(Estimate estimate, String traceId) {
        log.info("Publishing EstimateRejectedEvent for estimate: {}, workOrderId: {}", estimate.getId(), estimate.getWorkOrderId());
        Map<String, Object> payload = new HashMap<>();
        payload.put("estimateId", estimate.getId());
        payload.put("workOrderId", estimate.getWorkOrderId());
        payload.put("customerDocument", estimate.getCustomerDocument());
        payload.put("totalAmount", estimate.getTotalAmount().doubleValue());
        payload.put("status", estimate.getStatus().name());
        if (estimate.getRejectionReason() != null) {
            payload.put("reason", estimate.getRejectionReason());
        }

        send(estimate, "EstimateRejectedEvent", payload, traceId);
    }

    private void send(Estimate estimate, String eventType, Map<String, Object> payload, String traceId) {
        var envelope = EventEnvelope.of(
                eventType,
                SOURCE_SERVICE,
                traceId,
                payload
        );

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                BILLING_EVENTS_TOPIC,
                estimate.getWorkOrderId().toString(),
                envelope
        );

        if (traceId != null && !traceId.isBlank()) {
            record.headers().add(new RecordHeader(TRACE_HEADER, traceId.getBytes(StandardCharsets.UTF_8)));
        }

        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} for workOrderId: {}", eventType, estimate.getWorkOrderId(), ex);
            } else {
                log.info("Successfully published {} to topic {} with offset: {}",
                        eventType, BILLING_EVENTS_TOPIC, result.getRecordMetadata().offset());
            }
        });
    }
}
