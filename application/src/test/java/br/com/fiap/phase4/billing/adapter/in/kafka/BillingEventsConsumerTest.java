package br.com.fiap.phase4.billing.adapter.in.kafka;

import br.com.fiap.phase4.billing.domain.port.in.CreateInvoiceUseCase;
import br.com.fiap.phase4.billing.domain.port.in.ProcessMercadoPagoPaymentUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Billing Events Kafka Consumer Unit Tests")
class BillingEventsConsumerTest {

    @Mock
    private CreateInvoiceUseCase createInvoiceUseCase;

    @Mock
    private ProcessMercadoPagoPaymentUseCase processMercadoPagoPaymentUseCase;

    @InjectMocks
    private BillingEventsConsumer consumer;

    @Test
    @DisplayName("Should process valid WorkOrderApprovedEvent message and invoke CreateInvoiceUseCase")
    void shouldProcessWorkOrderApprovedEvent() {
        UUID workOrderId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "workOrderId", workOrderId.toString(),
                "customerDocument", "52998224725",
                "totalAmount", "350.00"
        );
        ConsumerRecord<String, Map<String, Object>> record =
                new ConsumerRecord<>("work-order-events", 0, 0L, workOrderId.toString(), payload);

        consumer.consumeWorkOrderApproved(record);

        verify(createInvoiceUseCase).create(eq(workOrderId), eq("52998224725"), eq(new BigDecimal("350.00")));
    }

    @Test
    @DisplayName("Should ignore incomplete WorkOrderApprovedEvent message")
    void shouldIgnoreIncompleteWorkOrderApprovedEvent() {
        Map<String, Object> payload = Map.of(
                "customerDocument", "52998224725"
        );
        ConsumerRecord<String, Map<String, Object>> record =
                new ConsumerRecord<>("work-order-events", 0, 0L, "key-1", payload);

        consumer.consumeWorkOrderApproved(record);

        verify(createInvoiceUseCase, never()).create(any(), anyString(), any());
    }

    @Test
    @DisplayName("Should process valid ExecutionFailedEvent message and invoke processCompensatingRefund")
    void shouldProcessExecutionFailedEvent() {
        UUID workOrderId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "workOrderId", workOrderId.toString(),
                "reason", "MECHANIC_UNAVAILABLE"
        );
        ConsumerRecord<String, Map<String, Object>> record =
                new ConsumerRecord<>("execution-events", 0, 0L, workOrderId.toString(), payload);

        consumer.consumeExecutionFailed(record, "trace-test-123".getBytes(StandardCharsets.UTF_8));

        verify(processMercadoPagoPaymentUseCase).processCompensatingRefund(
                eq(workOrderId), eq("MECHANIC_UNAVAILABLE"), eq("trace-test-123")
        );
    }

    @Test
    @DisplayName("Should ignore ExecutionStartedEvent on execution-events topic without triggering refund")
    void shouldIgnoreExecutionStartedEvent() {
        UUID workOrderId = UUID.randomUUID();
        Map<String, Object> message = Map.of(
                "metadata", Map.of("eventType", "ExecutionStartedEvent"),
                "payload", Map.of("workOrderId", workOrderId.toString())
        );
        ConsumerRecord<String, Map<String, Object>> record =
                new ConsumerRecord<>("execution-events", 0, 0L, workOrderId.toString(), message);

        consumer.consumeExecutionFailed(record, null);

        verify(processMercadoPagoPaymentUseCase, never()).processCompensatingRefund(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should ignore ExecutionCompletedEvent on execution-events topic without triggering refund")
    void shouldIgnoreExecutionCompletedEvent() {
        UUID workOrderId = UUID.randomUUID();
        Map<String, Object> message = Map.of(
                "metadata", Map.of("eventType", "ExecutionCompletedEvent"),
                "payload", Map.of("workOrderId", workOrderId.toString())
        );
        ConsumerRecord<String, Map<String, Object>> record =
                new ConsumerRecord<>("execution-events", 0, 0L, workOrderId.toString(), message);

        consumer.consumeExecutionFailed(record, null);

        verify(processMercadoPagoPaymentUseCase, never()).processCompensatingRefund(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should ignore non-WorkOrderApprovedEvent on work-order-events topic")
    void shouldIgnoreNonApprovedWorkOrderEvent() {
        UUID workOrderId = UUID.randomUUID();
        Map<String, Object> message = Map.of(
                "metadata", Map.of("eventType", "WorkOrderCreatedEvent"),
                "payload", Map.of(
                        "workOrderId", workOrderId.toString(),
                        "customerDocument", "52998224725",
                        "totalAmount", "350.00"
                )
        );
        ConsumerRecord<String, Map<String, Object>> record =
                new ConsumerRecord<>("work-order-events", 0, 0L, workOrderId.toString(), message);

        consumer.consumeWorkOrderApproved(record);

        verify(createInvoiceUseCase, never()).create(any(), anyString(), any());
    }
}
