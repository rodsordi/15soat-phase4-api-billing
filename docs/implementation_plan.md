# Plano de Implementação: Implementação da Camada Application com Entidades no Domínio

> **Decisão do Usuário**: Opção 2 - Mapeamento JPA Direto no Domínio (Padrão api-work-order e ADR 0001)
> **Status**: Aprovado para Execução

---

## 1. Escopo da Implementação

Este plano formaliza a implementação da camada `application` e o mapeamento das anotações de persistência JPA diretamente na entidade de domínio `Invoice`, em conformidade com a decisão do usuário e o padrão adotado no microsserviço irmão `15soat-phase4-api-work-order` e na ADR 0001.

---

## 2. Etapas de Execução

### Etapa 1: Atualização do Módulo de Domínio (`billing-domain`)
* Adicionar `jakarta.persistence-api` no `domain/pom.xml`.
* Anotar a classe `Invoice` com `@Entity`, `@Table(name = "invoice")`, `@Id`, `@Column`, `@Enumerated(EnumType.STRING)` e `@Version`.
* Preservar o encapsulamento e a imutabilidade dos métodos de domínio e Value Objects (`CpfOrCnpj`, `Money`).
* Garantir 100% dos testes unitários e ArchUnit passando sem quebras.

### Etapa 2: Implementação da Persistência na Camada `application`
* Criar interface `SpringDataInvoiceRepository extends JpaRepository<Invoice, UUID>` com métodos de consulta derivados:
  * `Optional<Invoice> findByWorkOrderId(UUID workOrderId);`
  * `Optional<Invoice> findByExternalPaymentId(String externalPaymentId);`
* Implementar o adaptador de saída `InvoiceRepositoryAdapter implements InvoiceRepositoryPort` delegando para o repositório JPA.

### Etapa 3: Integração HTTP Outbound com Mercado Pago
* Implementar o cliente HTTP e adaptador `MercadoPagoGatewayAdapter implements MercadoPagoGatewayPort`:
  * Criação de Checkout / QR Code PIX baseado na especificação `contracts/outbound/mercadopago-api.yaml`.
  * Consulta e reconciliação de status de pagamento (`PaymentVerificationResult`).
  * Processamento de estorno / reembolso (`refundPayment`).

### Etapa 4: Adaptadores de Mensageria Kafka (Entrada e Saída)
* Consumidor Kafka:
  * Consumir `WorkOrderApprovedEvent` do tópico `work-order-events` e chamar `CreateInvoiceUseCase`.
  * Consumir `ExecutionFailedEvent` do tópico `execution-events` e chamar `processCompensatingRefund`.
* Produtor Kafka (`BillingEventPublisherAdapter implements BillingEventPublisherPort`):
  * Publicar `PaymentConfirmedEvent` no tópico `payment-events`.
  * Publicar `PaymentFailedEvent` no tópico `payment-events`.

### Etapa 5: Adaptadores de Entrada REST (Controllers HTTP)
* Implementar `InvoiceController` e `PaymentWebhookController` aderentes ao `openapi.yaml`:
  * `POST /api/v1/invoices`
  * `GET /api/v1/invoices/{id}`
  * `GET /api/v1/invoices/work-order/{workOrderId}`
  * `POST /api/v1/payments/mercadopago/checkout`
  * `POST /api/v1/payments/mercadopago/webhook` (Idempotente)

### Etapa 6: Verificação & Quality Gate
* Executar build completo com Maven (`mvn clean test`).
* Validar cobertura de testes unitários e de integração de adaptadores.
