package br.com.fiap.phase4.billing.adapter.in.kafka;

import br.com.fiap.phase4.billing.domain.port.in.CreateInvoiceUseCase;
import br.com.fiap.phase4.billing.domain.port.out.InvoiceRepositoryPort;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort;
import br.com.fiap.phase4.commons.kafka.event.EventEnvelope;
import br.com.fiap.phase4.commons.kafka.tracing.KafkaTraceContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingKafkaConsumer {

    private final CreateInvoiceUseCase createInvoiceUseCase;
    private final InvoiceRepositoryPort invoiceRepository;
    private final MercadoPagoGatewayPort mercadoPagoGateway;

    @KafkaListener(topics = "work-order-events", groupId = "billing-work-order-group")
    public void consumeWorkOrderEvents(ConsumerRecord<String, EventEnvelope<Map<String, Object>>> record) {
        String traceId = KafkaTraceContextUtils.extractTraceId(record.headers());
        EventEnvelope<Map<String, Object>> envelope = record.value();

        if (envelope == null || envelope.metadata() == null) {
            return;
        }

        String eventType = envelope.metadata().eventType();
        log.info("Received event on work-order-events: {} [traceId: {}]", eventType, traceId);

        if ("WorkOrderApprovedEvent".equalsIgnoreCase(eventType)) {
            UUID workOrderId = UUID.fromString(envelope.payload().get("workOrderId").toString());
            String customerDocument = String.valueOf(envelope.payload().get("customerDocument"));
            BigDecimal totalAmount = new BigDecimal(envelope.payload().get("totalAmount").toString());

            log.info("Saga Step: Generating Invoice for approved Work Order {}", workOrderId);
            createInvoiceUseCase.create(workOrderId, customerDocument, totalAmount);
        }
    }

    @KafkaListener(topics = "execution-events", groupId = "billing-execution-group")
    public void consumeExecutionEvents(ConsumerRecord<String, EventEnvelope<Map<String, Object>>> record) {
        String traceId = KafkaTraceContextUtils.extractTraceId(record.headers());
        EventEnvelope<Map<String, Object>> envelope = record.value();

        if (envelope == null || envelope.metadata() == null) {
            return;
        }

        String eventType = envelope.metadata().eventType();
        log.info("Received event on execution-events: {} [traceId: {}]", eventType, traceId);

        if ("ExecutionFailedEvent".equalsIgnoreCase(eventType)) {
            UUID workOrderId = UUID.fromString(envelope.payload().get("workOrderId").toString());
            log.warn("Saga Rollback: Processing refund for aborted execution of Work Order {}", workOrderId);
            invoiceRepository.findByWorkOrderId(workOrderId).ifPresent(invoice -> {
                if (invoice.getExternalPaymentId() != null) {
                    mercadoPagoGateway.refundPayment(invoice.getExternalPaymentId());
                }
                invoice.refund();
                invoiceRepository.save(invoice);
            });
        }
    }
}
