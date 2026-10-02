package br.com.fiap.phase4.billing.domain.usecase;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.port.in.GetInvoiceUseCase;
import br.com.fiap.phase4.billing.domain.port.out.InvoiceRepositoryPort;
import br.com.fiap.phase4.commons.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetInvoiceUseCaseImpl implements GetInvoiceUseCase {

    private final InvoiceRepositoryPort repository;

    @Override
    public Invoice getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
    }

    @Override
    public Invoice getByWorkOrderId(UUID workOrderId) {
        return repository.findByWorkOrderId(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice for WorkOrder", workOrderId));
    }
}
