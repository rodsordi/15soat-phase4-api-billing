package br.com.fiap.phase4.billing.adapter.out.persistence;

import br.com.fiap.phase4.billing.adapter.out.persistence.repository.SpringDataInvoiceRepository;
import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.model.InvoiceStatus;
import br.com.fiap.phase4.commons.domain.vo.CpfOrCnpj;
import br.com.fiap.phase4.commons.domain.vo.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Invoice Repository Adapter Unit Tests")
class InvoiceRepositoryAdapterTest {

    @Mock
    private SpringDataInvoiceRepository repository;

    @InjectMocks
    private InvoiceRepositoryAdapter adapter;

    @Test
    @DisplayName("Should delegate save to SpringDataInvoiceRepository")
    void shouldDelegateSave() {
        Invoice invoice = Invoice.create(UUID.randomUUID(), new CpfOrCnpj("52998224725"), Money.of(100.00));
        when(repository.save(invoice)).thenReturn(invoice);

        Invoice saved = adapter.save(invoice);

        assertThat(saved).isSameAs(invoice);
        verify(repository).save(invoice);
    }

    @Test
    @DisplayName("Should delegate findById to SpringDataInvoiceRepository")
    void shouldDelegateFindById() {
        UUID id = UUID.randomUUID();
        Invoice invoice = Invoice.create(UUID.randomUUID(), new CpfOrCnpj("52998224725"), Money.of(100.00));
        when(repository.findById(id)).thenReturn(Optional.of(invoice));

        Optional<Invoice> result = adapter.findById(id);

        assertThat(result).contains(invoice);
        verify(repository).findById(id);
    }

    @Test
    @DisplayName("Should delegate findByWorkOrderId to SpringDataInvoiceRepository")
    void shouldDelegateFindByWorkOrderId() {
        UUID workOrderId = UUID.randomUUID();
        Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(100.00));
        when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(invoice));

        Optional<Invoice> result = adapter.findByWorkOrderId(workOrderId);

        assertThat(result).contains(invoice);
        verify(repository).findByWorkOrderId(workOrderId);
    }

    @Test
    @DisplayName("Should delegate findByExternalPaymentId to SpringDataInvoiceRepository")
    void shouldDelegateFindByExternalPaymentId() {
        String paymentId = "PAY-555";
        Invoice invoice = Invoice.create(UUID.randomUUID(), new CpfOrCnpj("52998224725"), Money.of(100.00));
        when(repository.findByExternalPaymentId(paymentId)).thenReturn(Optional.of(invoice));

        Optional<Invoice> result = adapter.findByExternalPaymentId(paymentId);

        assertThat(result).contains(invoice);
        verify(repository).findByExternalPaymentId(paymentId);
    }
}
