package br.com.fiap.phase4.billing.adapter.out.notification;

import br.com.fiap.phase4.billing.domain.model.Estimate;
import br.com.fiap.phase4.billing.domain.port.out.EstimateNotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EstimateNotificationAdapter implements EstimateNotificationPort {

    @Override
    public void sendEstimateProposalEmail(Estimate estimate) {
        String recipient = estimate.getCustomerEmail() != null ? estimate.getCustomerEmail() : "customer@" + estimate.getCustomerDocument() + ".com";
        String approvalLink = "https://oficina.fiap.com.br/estimates/" + estimate.getWorkOrderId() + "/approval";
        log.info("DISPATCHING EMAIL [ESTIMATE PROPOSAL] -> Recipient: {} | Work Order: {} | Total: R$ {} | Decision Link: {}",
                recipient, estimate.getWorkOrderId(), estimate.getTotalAmount(), approvalLink);
    }
}
