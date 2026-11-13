package br.com.fiap.phase4.billing.iandt;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.model.InvoiceStatus;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Billing Kafka Events Consumer Integration Tests")
class BillingEventsConsumerIntegrationTest extends BillingIntegrationTest {

    @Test
    @DisplayName("Should consume WorkOrderApprovedEvent from Kafka topic and create invoice in PostgreSQL")
    void shouldConsumeWorkOrderApprovedEventAndCreateInvoice() {
        UUID workOrderId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "workOrderId", workOrderId.toString(),
                "customerDocument", "52998224725",
                "totalAmount", "375.50"
        );

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                "work-order-events",
                workOrderId.toString(),
                payload
        );

        kafkaTemplate.send(record);

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Optional<Invoice> optionalInvoice = invoiceRepository.findByWorkOrderId(workOrderId);
                    assertThat(optionalInvoice).isPresent();
                    Invoice invoice = optionalInvoice.get();
                    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
                    assertThat(invoice.getCustomerDocument().value()).isEqualTo("52998224725");
                    assertThat(invoice.getAmount().amount().doubleValue()).isEqualTo(375.50);
                });
    }
}
