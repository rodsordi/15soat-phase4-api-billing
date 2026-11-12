package br.com.fiap.phase4.billing.adapter.out.persistence.repository;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataInvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByWorkOrderId(UUID workOrderId);

    Optional<Invoice> findByExternalPaymentId(String externalPaymentId);
}
