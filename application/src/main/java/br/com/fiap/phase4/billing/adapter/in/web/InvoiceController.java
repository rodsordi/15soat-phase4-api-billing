package br.com.fiap.phase4.billing.adapter.in.web;

import br.com.fiap.phase4.billing.adapter.in.web.api.BillingApi;
import br.com.fiap.phase4.billing.adapter.in.web.dto.CheckoutRequest;
import br.com.fiap.phase4.billing.adapter.in.web.dto.CheckoutResponse;
import br.com.fiap.phase4.billing.adapter.in.web.dto.CreateInvoiceRequest;
import br.com.fiap.phase4.billing.adapter.in.web.dto.InvoiceDetailedResponse;
import br.com.fiap.phase4.billing.adapter.in.web.mapper.InvoiceDtoMapper;
import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.billing.domain.port.in.CreateInvoiceUseCase;
import br.com.fiap.phase4.billing.domain.port.in.GetInvoiceUseCase;
import br.com.fiap.phase4.billing.domain.port.in.ProcessMercadoPagoPaymentUseCase;
import br.com.fiap.phase4.billing.domain.port.out.MercadoPagoGatewayPort.MercadoPagoCheckout;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class InvoiceController implements BillingApi {

    private final CreateInvoiceUseCase createInvoiceUseCase;
    private final GetInvoiceUseCase getInvoiceUseCase;
    private final ProcessMercadoPagoPaymentUseCase processMercadoPagoPaymentUseCase;

    @Override
    public ResponseEntity<InvoiceDetailedResponse> createInvoice(CreateInvoiceRequest request) {
        log.info("REST: Creating invoice for workOrderId: {}, customerDocument: {}, amount: {}",
                request.getWorkOrderId(), request.getCustomerDocument(), request.getAmount());

        Invoice invoice = createInvoiceUseCase.create(
                request.getWorkOrderId(),
                request.getCustomerDocument(),
                BigDecimal.valueOf(request.getAmount())
        );

        InvoiceDetailedResponse response = InvoiceDtoMapper.toDetailedResponse(invoice);
        return ResponseEntity
                .created(URI.create("/api/v1/invoices/" + invoice.getId()))
                .body(response);
    }

    @Override
    public ResponseEntity<InvoiceDetailedResponse> getInvoiceById(UUID id) {
        log.info("REST: Fetching invoice by ID: {}", id);
        Invoice invoice = getInvoiceUseCase.getById(id);
        return ResponseEntity.ok(InvoiceDtoMapper.toDetailedResponse(invoice));
    }

    @Override
    public ResponseEntity<InvoiceDetailedResponse> getInvoiceByWorkOrderId(UUID workOrderId) {
        log.info("REST: Fetching invoice by workOrderId: {}", workOrderId);
        Invoice invoice = getInvoiceUseCase.getByWorkOrderId(workOrderId);
        return ResponseEntity.ok(InvoiceDtoMapper.toDetailedResponse(invoice));
    }

    @Override
    public ResponseEntity<CheckoutResponse> createMercadoPagoCheckout(CheckoutRequest request) {
        log.info("REST: Generating Mercado Pago checkout for invoiceId: {}", request.getInvoiceId());
        MercadoPagoCheckout checkout = processMercadoPagoPaymentUseCase.generateCheckout(request.getInvoiceId());

        CheckoutResponse response = new CheckoutResponse()
                .invoiceId(request.getInvoiceId())
                .paymentId(checkout.paymentId())
                .qrCode(checkout.qrCode())
                .qrCodeBase64(checkout.qrCodeBase64())
                .status(checkout.status());

        return ResponseEntity.ok(response);
    }
}
