package br.com.fiap.phase4.billing.iandt;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.model.InvoiceStatus;
import br.com.fiap.phase4.commons.domain.vo.CpfOrCnpj;
import br.com.fiap.phase4.commons.domain.vo.Money;
import io.restassured.http.ContentType;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.ConsumerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@DisplayName("Payment Webhook Integration Tests with Kafka Outbound Verification")
class PaymentWebhookIntegrationTest extends BillingIntegrationTest {

    @Autowired
    private ConsumerFactory<String, Object> consumerFactory;

    @Test
    @DisplayName("Should process payment webhook, mark invoice as PAID, publish Kafka event, and maintain idempotency")
    void shouldProcessPaymentWebhookAndMarkAsPaid() {
        UUID workOrderId = UUID.randomUUID();
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(280.00));
        invoice.setExternalPaymentId(paymentId);
        invoiceRepository.saveAndFlush(invoice);

        Map<String, Object> webhookPayload = Map.of(
                "action", "payment.updated",
                "data", Map.of("id", paymentId)
        );

        // First notification - processes and confirms payment
        given()
                .contentType(ContentType.JSON)
                .body(webhookPayload)
        .when()
                .post("/api/v1/payments/mercadopago/webhook")
        .then()
                .statusCode(200)
                .body("status", equalTo("PROCESSED"));

        Optional<Invoice> paidInvoice = invoiceRepository.findByExternalPaymentId(paymentId);
        assertThat(paidInvoice).isPresent();
        assertThat(paidInvoice.get().getStatus()).isEqualTo(InvoiceStatus.PAID);

        // Second notification (retry) - idempotent response
        given()
                .contentType(ContentType.JSON)
                .body(webhookPayload)
        .when()
                .post("/api/v1/payments/mercadopago/webhook")
        .then()
                .statusCode(200)
                .body("status", equalTo("PROCESSED"));

        Optional<Invoice> stillPaidInvoice = invoiceRepository.findByExternalPaymentId(paymentId);
        assertThat(stillPaidInvoice).isPresent();
        assertThat(stillPaidInvoice.get().getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    @DisplayName("Should process payment webhook with rejection and mark invoice as FAILED")
    void shouldProcessPaymentWebhookWithRejection() {
        UUID workOrderId = UUID.randomUUID();
        String paymentId = "PAY-REJECT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(120.00));
        invoice.setExternalPaymentId(paymentId);
        invoiceRepository.saveAndFlush(invoice);

        Map<String, Object> webhookPayload = Map.of(
                "action", "payment.updated",
                "data", Map.of("id", paymentId)
        );

        given()
                .contentType(ContentType.JSON)
                .body(webhookPayload)
        .when()
                .post("/api/v1/payments/mercadopago/webhook")
        .then()
                .statusCode(200)
                .body("status", equalTo("PROCESSED"));

        Optional<Invoice> failedInvoice = invoiceRepository.findByExternalPaymentId(paymentId);
        assertThat(failedInvoice).isPresent();
        assertThat(failedInvoice.get().getStatus()).isEqualTo(InvoiceStatus.FAILED);
    }

    @Test
    @DisplayName("Should publish PaymentConfirmedEvent to payment-events Kafka topic upon approved payment webhook")
    void shouldPublishPaymentConfirmedEventToKafka() {
        UUID workOrderId = UUID.randomUUID();
        String paymentId = "PAY-KAFKA-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(350.00));
        invoice.setExternalPaymentId(paymentId);
        invoiceRepository.saveAndFlush(invoice);

        try (Consumer<String, Object> consumer = consumerFactory.createConsumer("test-group-" + UUID.randomUUID(), "test-client")) {
            consumer.subscribe(List.of("payment-events"));

            // Warm up partition assignment before triggering event
            consumer.poll(Duration.ofMillis(500));

            Map<String, Object> webhookPayload = Map.of(
                    "action", "payment.updated",
                    "data", Map.of("id", paymentId)
            );

            given()
                    .contentType(ContentType.JSON)
                    .body(webhookPayload)
            .when()
                    .post("/api/v1/payments/mercadopago/webhook")
            .then()
                    .statusCode(200)
                    .body("status", equalTo("PROCESSED"));

            List<ConsumerRecord<String, Object>> capturedRecords = new ArrayList<>();
            Awaitility.await()
                    .atMost(Duration.ofSeconds(15))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> {
                        ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(500));
                        records.forEach(capturedRecords::add);
                        assertThat(capturedRecords)
                                .anyMatch(record -> workOrderId.toString().equals(record.key()));
                    });
        }
    }
}
