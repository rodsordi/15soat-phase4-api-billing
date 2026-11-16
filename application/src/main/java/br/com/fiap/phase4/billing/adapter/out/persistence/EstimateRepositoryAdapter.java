package br.com.fiap.phase4.billing.adapter.out.persistence;

import br.com.fiap.phase4.billing.adapter.out.persistence.repository.SpringDataEstimateRepository;
import br.com.fiap.phase4.billing.domain.model.Estimate;
import br.com.fiap.phase4.billing.domain.port.out.EstimateRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EstimateRepositoryAdapter implements EstimateRepositoryPort {

    private final SpringDataEstimateRepository springDataEstimateRepository;

    @Override
    public Estimate save(Estimate estimate) {
        return springDataEstimateRepository.save(estimate);
    }

    @Override
    public Optional<Estimate> findById(UUID id) {
        return springDataEstimateRepository.findById(id);
    }

    @Override
    public Optional<Estimate> findByWorkOrderId(UUID workOrderId) {
        return springDataEstimateRepository.findByWorkOrderId(workOrderId);
    }

    @Override
    public boolean existsByWorkOrderId(UUID workOrderId) {
        return springDataEstimateRepository.existsByWorkOrderId(workOrderId);
    }
}
