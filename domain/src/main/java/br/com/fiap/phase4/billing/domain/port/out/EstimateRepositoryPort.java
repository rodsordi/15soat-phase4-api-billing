package br.com.fiap.phase4.billing.domain.port.out;

import br.com.fiap.phase4.billing.domain.model.Estimate;

import java.util.Optional;
import java.util.UUID;

public interface EstimateRepositoryPort {
    Estimate save(Estimate estimate);
    Optional<Estimate> findById(UUID id);
    Optional<Estimate> findByWorkOrderId(UUID workOrderId);
    boolean existsByWorkOrderId(UUID workOrderId);
}
