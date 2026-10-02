package br.com.fiap.phase4.billing.adapter.in.web;

import br.com.fiap.phase4.billing.adapter.in.web.api.BillingApi;
import br.com.fiap.phase4.billing.adapter.in.web.dto.CheckoutRequest;
import br.com.fiap.phase4.billing.adapter.in.web.dto.CheckoutResponse;
import br.com.fiap.phase4.billing.adapter.in.web.dto.CreateInvoiceRequest;
import br.com.fiap.phase4.billing.adapter.in.web.dto.InvoiceResponse;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BillingController implements BillingApi {

    private final CreateInvoiceUseCase createInvoiceUseCase;
    private final GetInvoiceUseCase getInvoiceUseCase;
    private final ProcessMercadoPagoPaymentUseCase processPaymentUseCase;

    @Override
    public ResponseEntity<InvoiceResponse> createInvoice(CreateInvoiceRequest request) {
        log.info("Received request to create invoice for work order: {}", request.getWorkOrderId());
        Invoice invoice = createInvoiceUseCase.create(
                request.getWorkOrderId(),
                request.getCustomerDocument(),
                BigDecimal.valueOf(request.getAmount())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(invoice));
    }

    @Override
    public ResponseEntity<InvoiceResponse> getInvoiceById(UUID id) {
        log.info("Received request to get invoice by ID: {}", id);
        Invoice invoice = getInvoiceUseCase.getById(id);
        return ResponseEntity.ok(toResponse(invoice));
    }

    @Override
    public ResponseEntity<InvoiceResponse> getInvoiceByWorkOrderId(UUID workOrderId) {
        log.info("Received request to get invoice by work order ID: {}", workOrderId);
        Invoice invoice = getInvoiceUseCase.getByWorkOrderId(workOrderId);
        return ResponseEntity.ok(toResponse(invoice));
    }

    @Override
    public ResponseEntity<CheckoutResponse> createMercadoPagoCheckout(CheckoutRequest request) {
        log.info("Received request to create Mercado Pago checkout for invoice: {}", request.getInvoiceId());
        MercadoPagoCheckout checkout = processPaymentUseCase.generateCheckout(request.getInvoiceId());

        CheckoutResponse response = new CheckoutResponse();
        response.setInvoiceId(request.getInvoiceId());
        response.setPaymentId(checkout.paymentId());
        response.setQrCode(checkout.qrCode());
        response.setQrCodeBase64(checkout.qrCodeBase64());
        response.setStatus(checkout.status());

        return ResponseEntity.ok(response);
    }

    private InvoiceResponse toResponse(Invoice domain) {
        InvoiceResponse response = new InvoiceResponse();
        response.setId(domain.getId());
        response.setWorkOrderId(domain.getWorkOrderId());
        response.setCustomerDocument(domain.getCustomerDocument().value());
        response.setAmount(domain.getAmount().amount().doubleValue());
        response.setStatus(domain.getStatus().name());
        response.setExternalPaymentId(domain.getExternalPaymentId());
        response.setCreatedAt(OffsetDateTime.ofInstant(domain.getCreatedAt(), ZoneOffset.UTC));
        response.setUpdatedAt(OffsetDateTime.ofInstant(domain.getUpdatedAt(), ZoneOffset.UTC));
        return response;
    }
}
