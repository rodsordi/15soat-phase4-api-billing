package br.com.fiap.phase4.billing.adapter.in.web;

import br.com.fiap.phase4.billing.adapter.in.web.api.EstimateApi;
import br.com.fiap.phase4.billing.adapter.in.web.dto.EstimateDetailedResponse;
import br.com.fiap.phase4.billing.adapter.in.web.dto.UpdateEstimateStatusRequest;
import br.com.fiap.phase4.billing.adapter.in.web.mapper.EstimateDtoMapper;
import br.com.fiap.phase4.billing.domain.model.Estimate;
import br.com.fiap.phase4.billing.domain.port.in.ApproveEstimateUseCase;
import br.com.fiap.phase4.billing.domain.port.in.GetEstimateUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EstimateController implements EstimateApi {

    private final GetEstimateUseCase getEstimateUseCase;
    private final ApproveEstimateUseCase approveEstimateUseCase;

    @Override
    public ResponseEntity<EstimateDetailedResponse> getEstimateByWorkOrderId(UUID workOrderId) {
        log.info("REST: Fetching estimate for workOrderId: {}", workOrderId);
        Estimate estimate = getEstimateUseCase.getByWorkOrderId(workOrderId);
        return ResponseEntity.ok(EstimateDtoMapper.toDetailedResponse(estimate));
    }

    @Override
    public ResponseEntity<EstimateDetailedResponse> updateEstimateStatus(UUID workOrderId, UpdateEstimateStatusRequest request) {
        log.info("REST: Updating estimate status for workOrderId: {}, targetStatus: {}", workOrderId, request.getStatus());
        String traceId = MDC.get("traceId");

        Estimate updated;
        if (request.getStatus() == UpdateEstimateStatusRequest.StatusEnum.APPROVED) {
            updated = approveEstimateUseCase.approveByWorkOrderId(workOrderId, traceId);
        } else if (request.getStatus() == UpdateEstimateStatusRequest.StatusEnum.REJECTED) {
            updated = approveEstimateUseCase.rejectByWorkOrderId(workOrderId, request.getReason(), traceId);
        } else {
            throw new IllegalArgumentException("Status must be either APPROVED or REJECTED");
        }

        return ResponseEntity.ok(EstimateDtoMapper.toDetailedResponse(updated));
    }
}
