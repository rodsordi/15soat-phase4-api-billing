package br.com.fiap.phase4.billing.domain.port.in;

import br.com.fiap.phase4.billing.domain.model.Invoice;

import java.util.UUID;

public interface GetInvoiceUseCase {

    Invoice getById(UUID id);

    Invoice getByWorkOrderId(UUID workOrderId);
}
