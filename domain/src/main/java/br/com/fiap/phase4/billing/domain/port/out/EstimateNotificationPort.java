package br.com.fiap.phase4.billing.domain.port.out;

import br.com.fiap.phase4.billing.domain.model.Estimate;

public interface EstimateNotificationPort {
    void sendEstimateProposalEmail(Estimate estimate);
}
