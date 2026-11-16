package br.com.fiap.phase4.billing.domain.usecase;

import br.com.fiap.phase4.billing.domain.model.Estimate;
import br.com.fiap.phase4.billing.domain.model.EstimateItem;
import br.com.fiap.phase4.billing.domain.port.in.CreateEstimateUseCase;
import br.com.fiap.phase4.billing.domain.port.out.EstimateEventPublisherPort;
import br.com.fiap.phase4.billing.domain.port.out.EstimateNotificationPort;
import br.com.fiap.phase4.billing.domain.port.out.EstimateRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateEstimateUseCaseImpl implements CreateEstimateUseCase {

    private final EstimateRepositoryPort repository;
    private final EstimateEventPublisherPort eventPublisher;
    private final EstimateNotificationPort notificationPort;

    @Override
    public Estimate createEstimate(UUID workOrderId, String customerDocument, String customerEmail, List<EstimateItem> items) {
        var existing = repository.findByWorkOrderId(workOrderId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Estimate estimate = Estimate.create(workOrderId, customerDocument, customerEmail, items);
        Estimate saved = repository.save(estimate);

        // 1. Send proposal email to customer with breakdown and decision link
        notificationPort.sendEstimateProposalEmail(saved);

        // 2. Publish event
        eventPublisher.publishEstimateCreated(saved, null);

        return saved;
    }
}
