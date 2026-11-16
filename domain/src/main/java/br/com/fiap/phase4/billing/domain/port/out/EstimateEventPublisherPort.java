package br.com.fiap.phase4.billing.domain.port.out;

import br.com.fiap.phase4.billing.domain.model.Estimate;

public interface EstimateEventPublisherPort {
    void publishEstimateCreated(Estimate estimate, String traceId);
    void publishEstimateApproved(Estimate estimate, String traceId);
    void publishEstimateRejected(Estimate estimate, String traceId);
}
