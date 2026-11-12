package br.com.fiap.phase4.billing.adapter.out.persistence;

import br.com.fiap.phase4.billing.adapter.out.persistence.repository.SpringDataInvoiceRepository;
import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.port.out.InvoiceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InvoiceRepositoryAdapter implements InvoiceRepositoryPort {

    private final SpringDataInvoiceRepository repository;

    @Override
    public Invoice save(Invoice invoice) {
        return repository.save(invoice);
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Invoice> findByWorkOrderId(UUID workOrderId) {
        return repository.findByWorkOrderId(workOrderId);
    }

    @Override
    public Optional<Invoice> findByExternalPaymentId(String externalPaymentId) {
        return repository.findByExternalPaymentId(externalPaymentId);
    }
}
