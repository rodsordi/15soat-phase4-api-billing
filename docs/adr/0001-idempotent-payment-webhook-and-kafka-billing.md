# ADR 0001: Processamento Idempotente de Webhooks de Pagamento e Publicação de Faturas

* **Status**: Aceito (Accepted)
* **Data**: 2026-10-02
* **Escopo**: Específico do Microsserviço `15soat-phase4-api-billing`
* **Autores**: FIAP SOAT Tech Challenge Team
* **Decisores Técnicos**: Especialistas em Engenharia de Software e Integração de Pagamentos

---

## 1. Contexto e Declaração do Problema

O microsserviço `15soat-phase4-api-billing` é responsável por receber solicitações de faturamento (via evento Kafka `work-order.approved`), gerar cobranças no gateway externo (Mercado Pago) e processar as notificações assíncronas de pagamento recebidas via HTTP Webhook.

Webhooks de provedores de pagamento apresentam desafios inerentes de mensageria em redes distribuídas:
1. **At-Least-Once Delivery**: O gateway externo pode retransmitir o mesmo webhook múltiplas vezes devido a timeouts de rede, retries automáticos ou indisponibilidades transitórias.
2. **Entregas Fora de Ordem**: Notificações de status ("pending" e "approved") podem chegar desordenadas.
3. **Duplicação de Eventos no Kafka**: Se um webhook for processado mais de uma vez sem idempotência, múltiplos eventos de `payment.confirmed` seriam publicados no Kafka, disparando indevidamente execuções duplicadas na oficina (`api-exec`).

---

## 2. Decisão Arquitetural

Decidimos implementar uma estratégia de **Processamento Idempotente e Controle Otimista de Concorrência** para a gestão de faturas e pagamentos:

1. **Rastreabilidade e Deduplicação via `payment_webhook`**:
   - Toda notificação recebida pelo endpoint `/api/v1/payments/webhook` é registrada na tabela `payment_webhook`.
   - O campo `external_event_id` (ID da notificação do Mercado Pago) em conjunto com `external_payment_id` garante unicidade e idempotência. Caso um webhook com o mesmo `external_event_id` já tenha sido processado com sucesso, o endpoint retorna HTTP 200 OK imediatamente sem reprocessar regras de negócio nem republicar eventos.

2. **Controle de Concorrência Otimista na `invoice` (`@Version`)**:
   - A entidade `Invoice` possui um campo `version` (mapeado via `@Version` do Jakarta Persistence).
   - Tentativas de alteração de status simultâneas (ex: cancelamento por timeout versus confirmação do webhook) são isoladas deterministicamente, prevenindo *lost updates*.

3. **Garantia de Transição de Estado Unidirecional (State Machine)**:
   - A fatura possui ciclo de vida estrito: `PENDING` -> `PAID` (ou `FAILED` / `CANCELED`).
   - Uma vez atingido o estado terminal `PAID`, qualquer webhook subsequente é ignorado de forma idempotente.

4. **Publicação Transacional de Eventos no Kafka**:
   - O disparo de `payment.confirmed` ou `payment.failed` ocorre somente após a persistência bem-sucedida da alteração de estado no banco PostgreSQL (`billing_db`).

---

## 3. Consequências

### Positivas:
* **Zero Duplicação de Cobrança e Execução**: Garante que uma fatura seja paga e confirmada exatamente uma vez na Saga coreografada.
* **Resiliência a Retries de Gateways Externos**: O Mercado Pago pode reenviar webhooks repetidamente sem causar efeitos colaterais na esteira de produção.
* **Auditabilidade Completa**: O histórico de todos os webhooks recebidos fica registrado na tabela `payment_webhook` para conciliação financeira e auditoria técnica.

### Negativas / Trade-offs:
* **Sobrecarga de Persistência**: A tabela `payment_webhook` cresce proporcionalmente ao volume de tentativas de pagamento, exigindo políticas futuras de expiração/retenção (TTL ou arquivamento frio de logs).
