package br.com.fiap.phase4.billing.iandt;

import br.com.fiap.phase4.billing.domain.model.Invoice;
import br.com.fiap.phase4.commons.domain.vo.CpfOrCnpj;
import br.com.fiap.phase4.commons.domain.vo.Money;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("Invoice Controller REST Integration Tests")
class InvoiceControllerIntegrationTest extends BillingIntegrationTest {

    @Test
    @DisplayName("Should create invoice via REST POST /api/v1/invoices (Happy Path)")
    void shouldCreateInvoiceViaRest() {
        UUID workOrderId = UUID.randomUUID();
        Map<String, Object> requestBody = Map.of(
                "workOrderId", workOrderId.toString(),
                "customerDocument", "52998224725",
                "amount", 199.99
        );

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/v1/invoices")
        .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("workOrderId", equalTo(workOrderId.toString()))
                .body("customerDocument", equalTo("52998224725"))
                .body("status", equalTo("PENDING"));
    }

    @Test
    @DisplayName("Should guarantee idempotency when duplicate invoice creation is submitted for same work order")
    void shouldGuaranteeIdempotencyWhenSubmittingDuplicateInvoiceRequest() {
        UUID workOrderId = UUID.randomUUID();
        Map<String, Object> requestBody = Map.of(
                "workOrderId", workOrderId.toString(),
                "customerDocument", "52998224725",
                "amount", 400.00
        );

        // First creation
        String firstInvoiceId = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/v1/invoices")
        .then()
                .statusCode(201)
                .extract().path("id");

        // Duplicate submission
        String duplicateInvoiceId = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/v1/invoices")
        .then()
                .statusCode(201)
                .extract().path("id");

        assertThat(duplicateInvoiceId).isEqualTo(firstInvoiceId);
        assertThat(invoiceRepository.findAll()).hasSize(1);
    }

    @ParameterizedTest(name = "Should reject invoice creation with amount: {0}")
    @ValueSource(doubles = {0.00, -50.00})
    @DisplayName("Should reject invoice creation when monetary amount is less than or equal to zero (HTTP 400)")
    void shouldRejectInvoiceWithZeroOrNegativeAmount(double invalidAmount) {
        UUID workOrderId = UUID.randomUUID();
        Map<String, Object> requestBody = Map.of(
                "workOrderId", workOrderId.toString(),
                "customerDocument", "52998224725",
                "amount", invalidAmount
        );

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/v1/invoices")
        .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should reject invoice creation when customer document is invalid (HTTP 422)")
    void shouldRejectInvoiceWithInvalidCustomerDocument() {
        UUID workOrderId = UUID.randomUUID();
        Map<String, Object> requestBody = Map.of(
                "workOrderId", workOrderId.toString(),
                "customerDocument", "11111111111",
                "amount", 250.00
        );

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/v1/invoices")
        .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("Should fetch invoice by ID via REST GET /api/v1/invoices/{id}")
    void shouldFetchInvoiceById() {
        UUID workOrderId = UUID.randomUUID();
        Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(450.00));
        Invoice saved = invoiceRepository.save(invoice);

        given()
                .contentType(ContentType.JSON)
        .when()
                .get("/api/v1/invoices/{id}", saved.getId())
        .then()
                .statusCode(200)
                .body("id", equalTo(saved.getId().toString()))
                .body("workOrderId", equalTo(workOrderId.toString()))
                .body("status", equalTo("PENDING"));
    }

    @Test
    @DisplayName("Should fetch invoice by workOrderId via REST GET /api/v1/invoices/work-order/{workOrderId} with HATEOAS links")
    void shouldFetchInvoiceByWorkOrderIdWithHateoasLinks() {
        UUID workOrderId = UUID.randomUUID();
        Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(500.00));
        Invoice saved = invoiceRepository.save(invoice);

        given()
                .contentType(ContentType.JSON)
        .when()
                .get("/api/v1/invoices/work-order/{workOrderId}", workOrderId)
        .then()
                .statusCode(200)
                .body("id", equalTo(saved.getId().toString()))
                .body("workOrderId", equalTo(workOrderId.toString()))
                .body("status", equalTo("PENDING"))
                .body("_links.self.href", equalTo("/api/v1/invoices/" + saved.getId()))
                .body("_links.workOrder.href", equalTo("/api/v1/work-orders/" + workOrderId));
    }

    @Test
    @DisplayName("Should return 404 Not Found when invoice ID does not exist")
    void shouldReturn404WhenInvoiceNotFound() {
        given()
                .contentType(ContentType.JSON)
        .when()
                .get("/api/v1/invoices/{id}", UUID.randomUUID())
        .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Should generate Mercado Pago Pix checkout for pending invoice via POST /api/v1/invoices/checkout")
    void shouldGenerateMercadoPagoPixCheckoutForPendingInvoice() {
        UUID workOrderId = UUID.randomUUID();
        Invoice invoice = Invoice.create(workOrderId, new CpfOrCnpj("52998224725"), Money.of(600.00));
        Invoice saved = invoiceRepository.save(invoice);

        Map<String, Object> checkoutRequestBody = Map.of(
                "invoiceId", saved.getId().toString()
        );

        given()
                .contentType(ContentType.JSON)
                .body(checkoutRequestBody)
        .when()
                .post("/api/v1/invoices/checkout")
        .then()
                .statusCode(200)
                .body("invoiceId", equalTo(saved.getId().toString()))
                .body("paymentId", notNullValue())
                .body("qrCode", notNullValue())
                .body("qrCodeBase64", notNullValue())
                .body("status", equalTo("created"));
    }
}
