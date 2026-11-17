# Plano de Implementação: Enriquecimento do billing-iandt com Cenários E2E

> **Decisão do Usuário**: Opção 1 - Implementar no billing-iandt todos os cenários funcionais, de borda e de mensageria aplicáveis ao Billing em JUnit 5 puro
> **Status**: Aprovado para Execução

---

## 1. Objetivo
Expandir a suíte de testes de integração do módulo `billing-iandt` no microsserviço `15soat-phase4-api-billing`, trazendo a cobertura dos cenários de teste definidos no Cucumber E2E (`invoice_generation.feature` e `payment_webhook_processing.feature`) para execução hermética e rápida com Testcontainers (PostgreSQL, Kafka e WireMock).

---

## 2. Escopo de Cenários Mapeados e Migrados

### 2.1. Cenários REST em `InvoiceControllerIntegrationTest`
1. **Idempotência na Emissão de Fatura (Cenário 2 E2E)**:
   - Requisição `POST /api/v1/invoices` duplicada com o mesmo `workOrderId`.
   - Garantir retorno da fatura já existente sem duplicidade de registro no PostgreSQL (`work_order_id UNIQUE`).
2. **Validações de Regra de Negócio e Domínio (Cenários 3 e 4 E2E - RFC 7807)**:
   - Requisição `POST /api/v1/invoices` com valor monetário $\le 0$ (`0.00` e `-50.00`) $\rightarrow$ HTTP 400 (`Bad Request` via `BillingExceptionHandler`).
   - Requisição `POST /api/v1/invoices` com documento de cliente inválido (`11111111111`) $\rightarrow$ HTTP 422 (`Unprocessable Entity` via `CommonsExceptionAdvice`).
3. **Consulta por Ordem de Serviço e Links HATEOAS (Cenário 5 E2E)**:
   - Requisição `GET /api/v1/invoices/work-order/{workOrderId}`.
   - Validação do retorno HTTP 200, status `PENDING` e links HAL (`_links.self.href` e `_links.workOrder.href`).
4. **Geração de Checkout Pix do Mercado Pago (Cenário 7 E2E)**:
   - Requisição `POST /api/v1/invoices/checkout` enviando `invoiceId`.
   - Validação do retorno HTTP 200 contendo `paymentId`, `qrCode` e `qrCodeBase64`.

### 2.2. Cenários de Mensageria e Webhook em `PaymentWebhookIntegrationTest`
1. **Auditoria de Publicação Outbound no Kafka (Cenário 1 e 2 E2E)**:
   - Consumo em tempo real do tópico Kafka `payment-events` utilizando `ConsumerFactory` de teste.
   - Quando o webhook processa pagamento aprovado: verificar que `PaymentConfirmedEvent` foi publicado com `invoiceId`, `workOrderId` e cabeçalho `X-Trace-Id`.
   - Quando o webhook processa pagamento rejeitado: verificar que `PaymentFailedEvent` foi publicado com o motivo da recusa.

---

## 3. Arquivos Alvo de Modificação
- `15soat-phase4-api-billing/iandt/src/test/java/br/com/fiap/phase4/billing/iandt/InvoiceControllerIntegrationTest.java`: Inclusão dos cenários de idempotência, validação de inputs (400 e 422), busca por workOrderId e geração de checkout Pix.
- `15soat-phase4-api-billing/iandt/src/test/java/br/com/fiap/phase4/billing/iandt/PaymentWebhookIntegrationTest.java`: Inclusão de verificação de publicação assíncrona de eventos Kafka.

---

## 4. Validação e Quality Gate
- `mvn clean test-compile -f 15soat-phase4-api-billing/pom.xml`: compilação 100% verde.
- `mvn test -f 15soat-phase4-api-billing/pom.xml`: suíte de testes de unidade verde.
- Código e assertivas estritamente em inglês.
