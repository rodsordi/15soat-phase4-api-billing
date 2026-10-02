package br.com.fiap.phase4.billing.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataInvoiceRepository extends JpaRepository<InvoiceJpaEntity, UUID> {

    Optional<InvoiceJpaEntity> findByWorkOrderId(UUID workOrderId);

    Optional<InvoiceJpaEntity> findByExternalPaymentId(String externalPaymentId);
}
