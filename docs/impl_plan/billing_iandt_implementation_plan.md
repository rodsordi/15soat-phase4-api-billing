# Plano de Implementação: Módulo de Testes de Integração (iandt) no api-billing

> **Decisão do Usuário**: Opção 1 - Criação do Submódulo Maven Dedicado iandt (billing-iandt) no 15soat-phase4-api-billing
> **Status**: Aprovado para Execução

---

## 1. Objetivo
Implementar o módulo físico de testes de integração `billing-iandt` (`iandt`) no microsserviço `15soat-phase4-api-billing`, baseado na referência canônica da Phase 3 (`api-garage.iandt`) e reutilizando as fixtures de Testcontainers (PostgreSQL, Kafka e WireMock) do `mod-integration-test` do `15soat-phase4-lib-commons`.

---

## 2. Escopo Técnico da Implementação

### 2.1. Configuração Maven do Parent (`15soat-phase4-api-billing/pom.xml`)
- Adicionar o submódulo `<module>iandt</module>` à lista de `<modules>`.
- Adicionar o artefato `billing-iandt` no `<dependencyManagement>`.
- Garantir que `mod-integration-test` e as dependências de testes estejam disponíveis.

### 2.2. Criação do Módulo `15soat-phase4-api-billing/iandt`
- Criar a estrutura de diretórios `iandt/src/test/java` e `iandt/src/test/resources`.
- Criar o arquivo `iandt/pom.xml`:
  - Dependência de compilação em `billing-application` e `billing-domain`.
  - Dependência em `mod-integration-test` (`<scope>test</scope>`).
  - Dependência em `spring-boot-starter-test` (`<scope>test</scope>`).
  - Dependência em `org.awaitility:awaitility` para asserções assíncronas do Kafka.
  - Perfil Maven `integrationTests` (`-DintegrationTests=true`) que desativa `<skipTests>` no surefire plugin (por padrão `<skipTests>true</skipTests>` para isolar builds unitários rápidos).

### 2.3. Arquivo de Configuração de Teste (`iandt/src/test/resources/application-test.yml`)
- Configuração do contexto Spring para os testes integrados:
  - `spring.jpa.hibernate.ddl-auto: validate`
  - `spring.flyway.enabled: true`
  - `spring.flyway.locations: classpath:db/migration`
  - `mercadopago.api.base-url: http://localhost:9090` (apontando para o WireMock na porta 9090)

### 2.4. Implementação da Classe Base (`BillingIntegrationTest.java`)
- Pacote: `br.com.fiap.phase4.billing.iandt`
- Anotações:
  - `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BillingApplication.class)`
  - `@ActiveProfiles("test")`
- Herança de Interfaces do Commons:
  - `implements PostgresSetup, KafkaSetup, WiremockSetup`
- Injeções de Infraestrutura:
  - `@LocalServerPort private Integer port;`
  - `SpringDataInvoiceRepository invoiceRepository;`
  - `SpringDataPaymentWebhookRepository webhookRepository;`
  - `KafkaTemplate<String, Object> kafkaTemplate;`
- Ciclo de Vida `@BeforeEach`:
  - Configuração do RestAssured: `RestAssured.baseURI = String.format("http://localhost:%s", port);`
  - Limpeza determinística das tabelas do PostgreSQL (`deleteAll()`).
  - Reset dos stubs do WireMock.

### 2.5. Suítes de Testes de Integração Concretas
1. **`InvoicePersistenceIntegrationTest.java`**:
   - Validação da persistência real de `InvoiceJpaEntity` no PostgreSQL.
   - Validação dos scripts Flyway (criação de tabelas e constraints de banco).
   - Validação do controle de concorrência otimista (`@Version`).
2. **`BillingEventsConsumerIntegrationTest.java`**:
   - Envio de evento `WorkOrderApprovedEvent` real para o tópico Kafka `work-order-events`.
   - Verificação com Awaitility do consumo assíncrono pelo `BillingEventsConsumer`.
   - Confirmação de que a fatura foi salva no PostgreSQL no status `PENDING`.
3. **`PaymentWebhookIntegrationTest.java`**:
   - Configuração do stub no WireMock para mockar a API do Mercado Pago (`GET /v1/payments/{id}`).
   - Disparo de requisição HTTP POST para `/api/v1/payments/webhook` via RestAssured.
   - Verificação de persistência na tabela `payment_webhook` (ADR 0001).
   - Confirmação da transição de estado da fatura para `PAID`.
   - Verificação de idempotência retransmitindo o mesmo payload (retorno HTTP 200 sem duplicidade).
4. **`InvoiceControllerIntegrationTest.java`**:
   - Criação prévia de fatura no banco.
   - Requisição HTTP GET para `/api/v1/invoices/{id}` via RestAssured.
   - Validação do payload retornado e do status HTTP 200 / 404 (RFC 7807).

### 2.6. Quality Gate e Validação
- Compilação completa com `mvn clean compile` no reactor do `15soat-phase4-api-billing`.
- Execução de testes de unidade sem interferência.
- Execução dos testes integrados com Docker ativo via `mvn test -DintegrationTests=true -pl iandt`.
