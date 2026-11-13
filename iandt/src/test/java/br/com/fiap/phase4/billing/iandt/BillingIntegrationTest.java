package br.com.fiap.phase4.billing.iandt;

import br.com.fiap.phase4.billing.BillingApplication;
import br.com.fiap.phase4.billing.adapter.out.persistence.repository.SpringDataInvoiceRepository;
import br.com.fiap.phase4.commons.integration.setup.KafkaSetup;
import br.com.fiap.phase4.commons.integration.setup.PostgresSetup;
import br.com.fiap.phase4.commons.integration.setup.WiremockSetup;
import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import static java.lang.String.format;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = BillingApplication.class
)
public abstract class BillingIntegrationTest implements PostgresSetup, KafkaSetup, WiremockSetup {

    @LocalServerPort
    protected Integer port;

    @Autowired
    protected SpringDataInvoiceRepository invoiceRepository;

    @Autowired
    protected KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUpBase() {
        RestAssured.baseURI = format("http://localhost:%s", port);

        log.info("Cleaning invoice table before integration test execution");
        invoiceRepository.deleteAll();
    }
}
