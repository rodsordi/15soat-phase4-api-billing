package br.com.fiap.phase4.billing.domain.port.in;

import br.com.fiap.phase4.billing.domain.model.Estimate;

import java.util.UUID;

public interface ApproveEstimateUseCase {
    Estimate approveByWorkOrderId(UUID workOrderId, String traceId);
    Estimate rejectByWorkOrderId(UUID workOrderId, String reason, String traceId);
}
