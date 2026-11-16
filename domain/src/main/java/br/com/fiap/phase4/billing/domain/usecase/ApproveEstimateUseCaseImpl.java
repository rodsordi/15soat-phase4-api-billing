package br.com.fiap.phase4.billing.domain.usecase;

import br.com.fiap.phase4.billing.domain.model.Estimate;
import br.com.fiap.phase4.billing.domain.port.in.ApproveEstimateUseCase;
import br.com.fiap.phase4.billing.domain.port.out.EstimateEventPublisherPort;
import br.com.fiap.phase4.billing.domain.port.out.EstimateRepositoryPort;
import br.com.fiap.phase4.commons.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApproveEstimateUseCaseImpl implements ApproveEstimateUseCase {

    private final EstimateRepositoryPort repository;
    private final EstimateEventPublisherPort eventPublisher;

    @Override
    public Estimate approveByWorkOrderId(UUID workOrderId, String traceId) {
        Estimate estimate = repository.findByWorkOrderId(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate", workOrderId));

        estimate.approve();
        Estimate updated = repository.save(estimate);

        // Publish event to release Work Order via Kafka
        eventPublisher.publishEstimateApproved(updated, traceId);

        return updated;
    }

    @Override
    public Estimate rejectByWorkOrderId(UUID workOrderId, String reason, String traceId) {
        Estimate estimate = repository.findByWorkOrderId(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate", workOrderId));

        estimate.reject(reason);
        Estimate updated = repository.save(estimate);

        // Publish event to cancel Work Order via Kafka
        eventPublisher.publishEstimateRejected(updated, traceId);

        return updated;
    }
}
