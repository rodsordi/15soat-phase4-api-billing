package br.com.fiap.phase4.billing.domain.usecase;

import br.com.fiap.phase4.billing.domain.model.Estimate;
import br.com.fiap.phase4.billing.domain.port.in.GetEstimateUseCase;
import br.com.fiap.phase4.billing.domain.port.out.EstimateRepositoryPort;
import br.com.fiap.phase4.commons.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetEstimateUseCaseImpl implements GetEstimateUseCase {

    private final EstimateRepositoryPort repository;

    @Override
    public Estimate getByWorkOrderId(UUID workOrderId) {
        return repository.findByWorkOrderId(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate", workOrderId));
    }

    @Override
    public Estimate getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate", id));
    }
}
