package br.com.fiap.phase4.billing.domain.port.in;

import br.com.fiap.phase4.billing.domain.model.Invoice;

import java.math.BigDecimal;
import java.util.UUID;

public interface CreateInvoiceUseCase {

    Invoice create(UUID workOrderId, String customerDocument, BigDecimal amount);
}
