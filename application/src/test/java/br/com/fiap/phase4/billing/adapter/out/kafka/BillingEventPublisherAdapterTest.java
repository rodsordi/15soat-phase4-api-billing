package br.com.fiap.phase4.billing.adapter.out.kafka;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.commons.domain.vo.CpfOrCnpj;
import br.com.fiap.phase4.commons.domain.vo.Money;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Billing Event Publisher Adapter Unit Tests")
class BillingEventPublisherAdapterTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private BillingEventPublisherAdapter publisher;

    @Test
    @DisplayName("Should publish PaymentConfirmedEvent with traceId header")
    void shouldPublishPaymentConfirmed() {
        UUID workOrderId = UUID.randomUUID();
        Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(400.00));

        RecordMetadata metadata = new RecordMetadata(new TopicPartition("payment-events", 0), 0, 0, 0, 0, 0);
        SendResult<String, Object> sendResult = new SendResult<>(null, metadata);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(sendResult));

        publisher.publishPaymentConfirmed(invoice, "trace-abc");

        ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());

        ProducerRecord<String, Object> record = captor.getValue();
        assertThat(record.topic()).isEqualTo("payment-events");
        assertThat(record.key()).isEqualTo(workOrderId.toString());
        assertThat(record.value()).isInstanceOf(Map.class);
        Map<?, ?> map = (Map<?, ?>) record.value();
        assertThat(map.get("invoiceId")).isEqualTo(invoice.getId());
        assertThat(map.get("workOrderId")).isEqualTo(workOrderId);
        assertThat(map.get("amount")).isEqualTo(400.00);
        assertThat(record.headers().lastHeader("X-Trace-Id")).isNotNull();
    }

    @Test
    @DisplayName("Should publish PaymentFailedEvent with reason")
    void shouldPublishPaymentFailed() {
        UUID workOrderId = UUID.randomUUID();
        Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(150.00));

        RecordMetadata metadata = new RecordMetadata(new TopicPartition("payment-events", 0), 0, 0, 0, 0, 0);
        SendResult<String, Object> sendResult = new SendResult<>(null, metadata);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(sendResult));

        publisher.publishPaymentFailed(invoice, "CARD_DECLINED", "trace-xyz");

        ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());

        ProducerRecord<String, Object> record = captor.getValue();
        assertThat(record.topic()).isEqualTo("payment-events");
        assertThat(record.key()).isEqualTo(workOrderId.toString());
        assertThat(record.value()).isInstanceOf(Map.class);
        Map<?, ?> map = (Map<?, ?>) record.value();
        assertThat(map.get("workOrderId")).isEqualTo(workOrderId);
        assertThat(map.get("reason")).isEqualTo("CARD_DECLINED");
    }
}
