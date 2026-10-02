package br.com.fiap.phase4.billing.domain.port.out;

import br.com.fiap.phase4.billing.domain.model.Invoice;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepositoryPort {

    Invoice save(Invoice invoice);

    Optional<Invoice> findById(UUID id);

    Optional<Invoice> findByWorkOrderId(UUID workOrderId);

    Optional<Invoice> findByExternalPaymentId(String externalPaymentId);
}
