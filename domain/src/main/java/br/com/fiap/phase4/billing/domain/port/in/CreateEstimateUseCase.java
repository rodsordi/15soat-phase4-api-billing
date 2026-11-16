package br.com.fiap.phase4.billing.domain.port.in;

import br.com.fiap.phase4.billing.domain.model.Estimate;
import br.com.fiap.phase4.billing.domain.model.EstimateItem;

import java.util.List;
import java.util.UUID;

public interface CreateEstimateUseCase {
    Estimate createEstimate(UUID workOrderId, String customerDocument, String customerEmail, List<EstimateItem> items);
}
