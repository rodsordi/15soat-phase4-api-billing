package br.com.fiap.phase4.billing.domain.port.in;

import br.com.fiap.phase4.billing.domain.model.Estimate;

import java.util.UUID;

public interface GetEstimateUseCase {
    Estimate getByWorkOrderId(UUID workOrderId);
    Estimate getById(UUID id);
}
