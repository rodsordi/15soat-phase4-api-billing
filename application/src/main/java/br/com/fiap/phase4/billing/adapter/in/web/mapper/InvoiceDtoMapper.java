package br.com.fiap.phase4.billing.adapter.in.web.mapper;

import br.com.fiap.phase4.billing.adapter.in.web.dto.InvoiceDetailedResponse;
import br.com.fiap.phase4.billing.adapter.in.web.dto.InvoiceDetailedResponseLinks;
import br.com.fiap.phase4.billing.adapter.in.web.dto.Link;
import br.com.fiap.phase4.billing.domain.model.Invoice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class InvoiceDtoMapper {

    private InvoiceDtoMapper() {}

    public static InvoiceDetailedResponse toDetailedResponse(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        InvoiceDetailedResponse response = new InvoiceDetailedResponse();
        response.setId(invoice.getId());
        response.setWorkOrderId(invoice.getWorkOrderId());
        response.setCustomerDocument(invoice.getRawCustomerDocument() != null
                ? invoice.getRawCustomerDocument()
                : (invoice.getCustomerDocument() != null ? invoice.getCustomerDocument().value() : null));
        response.setAmount(invoice.getRawAmount() != null
                ? invoice.getRawAmount().doubleValue()
                : (invoice.getAmount() != null ? invoice.getAmount().amount().doubleValue() : null));
        response.setStatus(invoice.getStatus() != null ? invoice.getStatus().name() : null);
        response.setExternalPaymentId(invoice.getExternalPaymentId());

        if (invoice.getCreatedAt() != null) {
            response.setCreatedAt(OffsetDateTime.ofInstant(invoice.getCreatedAt(), ZoneOffset.UTC));
        }
        if (invoice.getUpdatedAt() != null) {
            response.setUpdatedAt(OffsetDateTime.ofInstant(invoice.getUpdatedAt(), ZoneOffset.UTC));
        }

        response.setVersion(invoice.getVersion());

        // HATEOAS HAL Links
        InvoiceDetailedResponseLinks links = new InvoiceDetailedResponseLinks();
        if (invoice.getId() != null) {
            links.setSelf(new Link("/api/v1/invoices/" + invoice.getId()));
        }
        if (invoice.getWorkOrderId() != null) {
            links.setWorkOrder(new Link("/api/v1/work-orders/" + invoice.getWorkOrderId()));
        }
        response.setLinks(links);

        return response;
    }
}
