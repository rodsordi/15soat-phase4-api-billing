package br.com.fiap.phase4.billing.adapter.in.web.mapper;

import br.com.fiap.phase4.billing.adapter.in.web.dto.EstimateDetailedResponse;
import br.com.fiap.phase4.billing.adapter.in.web.dto.EstimateItemResponse;
import br.com.fiap.phase4.billing.domain.model.Estimate;
import br.com.fiap.phase4.billing.domain.model.EstimateItem;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

public final class EstimateDtoMapper {

    private EstimateDtoMapper() {}

    public static EstimateDetailedResponse toDetailedResponse(Estimate estimate) {
        if (estimate == null) {
            return null;
        }

        EstimateDetailedResponse response = new EstimateDetailedResponse();
        response.setId(estimate.getId());
        response.setWorkOrderId(estimate.getWorkOrderId());
        response.setCustomerDocument(estimate.getCustomerDocument());
        response.setCustomerEmail(estimate.getCustomerEmail());
        response.setTotalAmount(estimate.getTotalAmount() != null ? estimate.getTotalAmount().doubleValue() : 0.0);
        response.setStatus(estimate.getStatus() != null
                ? EstimateDetailedResponse.StatusEnum.fromValue(estimate.getStatus().name())
                : null);
        response.setRejectionReason(estimate.getRejectionReason());

        if (estimate.getItems() != null) {
            response.setItems(estimate.getItems().stream().map(EstimateDtoMapper::toItemResponse).toList());
        } else {
            response.setItems(Collections.emptyList());
        }

        if (estimate.getCreatedAt() != null) {
            response.setCreatedAt(OffsetDateTime.ofInstant(estimate.getCreatedAt(), ZoneOffset.UTC));
        }
        if (estimate.getUpdatedAt() != null) {
            response.setUpdatedAt(OffsetDateTime.ofInstant(estimate.getUpdatedAt(), ZoneOffset.UTC));
        }
        response.setVersion(estimate.getVersion());

        return response;
    }

    public static EstimateItemResponse toItemResponse(EstimateItem item) {
        if (item == null) {
            return null;
        }

        EstimateItemResponse response = new EstimateItemResponse();
        response.setId(item.getId());
        response.setItemType(item.getItemType() != null
                ? EstimateItemResponse.ItemTypeEnum.fromValue(item.getItemType().name())
                : null);
        response.setItemCodeOrSku(item.getItemCodeOrSku());
        response.setName(item.getName());
        response.setUnitPrice(item.getUnitPrice() != null ? item.getUnitPrice().doubleValue() : 0.0);
        response.setQuantity(item.getQuantity());
        response.setSubtotal(item.getSubtotal() != null ? item.getSubtotal().doubleValue() : 0.0);

        return response;
    }
}
