package br.com.fiap.phase4.billing.iandt;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.model.InvoiceStatus;
import br.com.fiap.phase4.commons.domain.vo.CpfOrCnpj;
import br.com.fiap.phase4.commons.domain.vo.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Invoice Persistence Integration Tests with PostgreSQL Testcontainers")
class InvoicePersistenceIntegrationTest extends BillingIntegrationTest {

    @Test
    @DisplayName("Should successfully persist and retrieve invoice from real PostgreSQL database")
    void shouldPersistAndRetrieveInvoice() {
        UUID workOrderId = UUID.randomUUID();
        Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(250.00));

        Invoice saved = invoiceRepository.save(invoice);
        assertThat(saved.getId()).isNotNull();

        Optional<Invoice> found = invoiceRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getWorkOrderId()).isEqualTo(workOrderId);
        assertThat(found.get().getStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(found.get().getCustomerDocument().value()).isEqualTo("52998224725");
        assertThat(found.get().getAmount().amount().doubleValue()).isEqualTo(250.00);
    }

    @Test
    @DisplayName("Should find invoice by workOrderId in PostgreSQL")
    void shouldFindInvoiceByWorkOrderId() {
        UUID workOrderId = UUID.randomUUID();
        Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(150.00));
        invoiceRepository.save(invoice);

        Optional<Invoice> found = invoiceRepository.findByWorkOrderId(workOrderId);
        assertThat(found).isPresent();
        assertThat(found.get().getWorkOrderId()).isEqualTo(workOrderId);
    }

    @Test
    @DisplayName("Should update invoice status and increment optimistic locking version")
    void shouldUpdateInvoiceStatusAndIncrementVersion() {
        UUID workOrderId = UUID.randomUUID();
        Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(300.00));
        Invoice saved = invoiceRepository.saveAndFlush(invoice);
        Long initialVersion = saved.getVersion();

        saved.markAsPaid("PAY-TEST-12345");
        Invoice updated = invoiceRepository.saveAndFlush(saved);

        assertThat(updated.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(updated.getExternalPaymentId()).isEqualTo("PAY-TEST-12345");
        assertThat(updated.getVersion()).isGreaterThan(initialVersion);
    }
}
