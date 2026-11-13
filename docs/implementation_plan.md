# Plano de Implementação: Filtragem Seletiva de Eventos no Billing Consumer

> **Decisão do Usuário**: Opção 1 - Filtragem Defensiva por Discriminador eventType em BillingEventsConsumer
> **Status**: Aprovado para Execução

---

## 1. Contexto e Motivação
Durante a execução da suíte E2E em ambiente `prd`, o cenário de retransmissão de webhook (`payment_webhook_processing.feature`) falhou porque a fatura foi indevidamente transicionada para `REFUNDED`.
A investigação revelou que o método `consumeExecutionFailed` em `BillingEventsConsumer.java` consome o tópico `execution-events` sem filtrar pelo discriminador `eventType`. Dessa forma, o evento `ExecutionStartedEvent` (emitido quando a ordem de serviço inicia a execução) foi interpretado como `ExecutionFailedEvent`, disparando o estorno compensatório.

---

## 2. Escopo das Alterações

### 2.1. `15soat-phase4-api-billing`
- **Arquivo**: `application/src/main/java/br/com/fiap/phase4/billing/adapter/in/kafka/BillingEventsConsumer.java`
  - Implementar método auxiliar `extractEventType(Map<String, Object> body)`.
  - No método `consumeExecutionFailed`, verificar se `eventType != null && !"ExecutionFailedEvent".equals(eventType)`. Se for diferente, registrar log em nível debug e descartar a mensagem sem acionar o use case de estorno.
  - No método `consumeWorkOrderApproved`, verificar se `eventType != null && !"WorkOrderApprovedEvent".equals(eventType)`. Se for diferente, descartar a mensagem sem acionar a criação de fatura.
- **Arquivo**: `application/src/test/java/br/com/fiap/phase4/billing/adapter/in/kafka/BillingEventsConsumerTest.java`
  - Adicionar teste unitário validando que eventos com `eventType="ExecutionStartedEvent"` ou `eventType="ExecutionCompletedEvent"` são ignorados por `consumeExecutionFailed`.
  - Adicionar teste unitário validando que eventos não correspondentes a `WorkOrderApprovedEvent` são ignorados por `consumeWorkOrderApproved`.

---

## 3. Validação e Quality Gate
1. Executar suíte de testes unitários localmente via Maven:
   ```powershell
   mvn test -f C:\git\fiap\phase4\15soat-phase4-api-billing\pom.xml
   ```
2. Submeter ao auditor sentinela de código.
3. Solicitar autorização do usuário para commit, push e abertura de PR.
4. Após merge e conclusão da pipeline CI/CD, executar a suíte E2E completa para validar 53/53 cenários aprovados.
