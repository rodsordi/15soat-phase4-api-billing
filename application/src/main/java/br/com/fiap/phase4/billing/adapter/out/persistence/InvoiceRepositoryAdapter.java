package br.com.fiap.phase4.billing.adapter.out.persistence;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.port.out.InvoiceRepositoryPort;
import br.com.fiap.phase4.commons.domain.vo.CpfOrCnpj;
import br.com.fiap.phase4.commons.domain.vo.Money;
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
        InvoiceJpaEntity entity = toEntity(invoice);
        InvoiceJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Invoice> findByWorkOrderId(UUID workOrderId) {
        return repository.findByWorkOrderId(workOrderId).map(this::toDomain);
    }

    @Override
    public Optional<Invoice> findByExternalPaymentId(String externalPaymentId) {
        return repository.findByExternalPaymentId(externalPaymentId).map(this::toDomain);
    }

    private InvoiceJpaEntity toEntity(Invoice domain) {
        InvoiceJpaEntity entity = new InvoiceJpaEntity(
                domain.getId(),
                domain.getWorkOrderId(),
                domain.getCustomerDocument().value(),
                domain.getAmount().amount(),
                domain.getStatus(),
                domain.getExternalPaymentId()
        );
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    private Invoice toDomain(InvoiceJpaEntity entity) {
        return new Invoice(
                entity.getId(),
                entity.getWorkOrderId(),
                new CpfOrCnpj(entity.getCustomerDocument()),
                Money.of(entity.getAmount()),
                entity.getStatus(),
                entity.getExternalPaymentId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
