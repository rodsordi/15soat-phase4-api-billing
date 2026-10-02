package br.com.fiap.phase4.billing.domain.usecase;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.port.in.CreateInvoiceUseCase;
import br.com.fiap.phase4.billing.domain.port.out.InvoiceRepositoryPort;
import br.com.fiap.phase4.commons.domain.vo.CpfOrCnpj;
import br.com.fiap.phase4.commons.domain.vo.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateInvoiceUseCaseImpl implements CreateInvoiceUseCase {

    private final InvoiceRepositoryPort repository;

    @Override
    public Invoice create(UUID workOrderId, String customerDocument, BigDecimal amount) {
        // If an invoice already exists for this workOrderId, return it (idempotency)
        var existing = repository.findByWorkOrderId(workOrderId);
        if (existing.isPresent()) {
            return existing.get();
        }

        var doc = new CpfOrCnpj(customerDocument);
        var money = Money.of(amount);
        var invoice = Invoice.create(workOrderId, doc, money);

        return repository.save(invoice);
    }
}
