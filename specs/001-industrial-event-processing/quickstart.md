# Quickstart & Validation Guide: Industrial Event Ingestion and Processing

**Feature**: [001-industrial-event-processing](spec.md) | **Date**: 2026-10-24

Este guia apresenta o roteiro para execução local e validação prática de ponta a ponta dos cenários de negócio estabelecidos na especificação.

---

## 1. Pré-Requisitos do Ambiente

- **Java**: JDK 17+ instalado e configurado (`java -version`).
- **Maven**: 3.8+ (`mvn -version`).
- **Node.js & npm**: Node 18+ e npm 9+ (`node -v`, `npm -v`).
- **Docker & Docker Compose**: Para execução dos contêineres do PostgreSQL e da stack completa.

---

## 2. Inicialização dos Serviços

### Opção A: Execução Local Rápida (Desenvolvimento)

1. **Subir o Banco de Dados PostgreSQL**:
   ```bash
   docker run --name eventhub-postgres -e POSTGRES_DB=eventhub -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16-alpine
   ```

2. **Iniciar o Backend (Spring Boot na porta 8080)**:
   ```bash
   cd backend
   mvn spring-boot:run
   ```

3. **Iniciar o Frontend (Angular na porta 4200)**:
   ```bash
   cd frontend
   npm install
   npm start
   ```

### Opção B: Orquestração Completa via Docker Compose

```bash
docker compose up --build -d
```
- Frontend: `http://localhost:4200`
- Backend API: `http://localhost:8080/api/events`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Actuator Health: `http://localhost:8080/actuator/health`

---

## 3. Roteiro de Validação dos Cenários de Negócio

### Cenário 1: Ingestão de Evento de Produção (`PRODUCTION`)

Submeter um evento de produção padrão:
```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "EVT-1001",
    "eventType": "PRODUCTION",
    "description": "Linha de montagem A1 iniciada",
    "timestamp": "2026-05-20T10:00:00"
  }'
```
- **Resultado Esperado**: Código HTTP `201 Created` retornando o JSON do evento persistido.

---

### Cenário 2: Validação de Idempotência e Bloqueio de Duplicidade

Reenviar exatamente a mesma requisição do Cenário 1 (mesmo `eventId` "EVT-1001"):
```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "EVT-1001",
    "eventType": "PRODUCTION",
    "description": "Linha de montagem A1 reiniciada",
    "timestamp": "2026-05-20T10:05:00"
  }'
```
- **Resultado Esperado**: Código HTTP `409 Conflict` com mensagem clara indicando que o evento `EVT-1001` já foi processado anteriormente.

---

### Cenário 3: Ingestão de Evento de Qualidade (`QUALITY`) com Auditoria

Submeter um evento classificado como qualidade:
```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "EVT-2001",
    "eventType": "QUALITY",
    "description": "Inspeção dimensional acusou desvio crítico no lote 99"
  }'
```
- **Resultado Esperado**:
  - Código HTTP `201 Created`.
  - O campo `timestamp` é preenchido automaticamente com o horário do servidor (validando o fallback).
  - Um registro de auditoria é gravado na tabela `audit_logs` com a ação `QUALITY_EVENT_REGISTERED`.

---

### Cenário 4: Consulta Paginada e Ordenação Decrescente

Consultar a listagem de eventos:
```bash
curl -X GET "http://localhost:8080/api/events?page=0&size=10"
```
- **Resultado Esperado**: Código HTTP `200 OK` trazendo o evento `EVT-2001` no topo da lista (mais recente primeiro), seguido por `EVT-1001`.

---

### Cenário 5: Verificação de Observabilidade & Métricas de Negócio

Consultar as métricas no Spring Boot Actuator:
```bash
curl -u admin:admin http://localhost:8080/actuator/metrics/events.processed
curl -u admin:admin http://localhost:8080/actuator/metrics/events.duplicate
```
- **Resultado Esperado**: Contadores refletindo exatamente os eventos processados e as duplicidades rejeitadas.

---

### Cenário 6: Validação Visual no Frontend Angular

1. Acessar `http://localhost:4200` no navegador.
2. Preencher o formulário com `eventId: EVT-3001`, `eventType: PRODUCTION` e `description: Teste via interface`. Clicar em "Enviar".
   - **Resultado**: Alerta visual verde de sucesso exibido e a tabela é atualizada imediatamente com o novo evento no topo.
3. Tentar enviar novamente `EVT-3001` pelo formulário.
   - **Resultado**: Alerta visual vermelho de erro destacando o conflito de duplicidade (`409 Conflict`).

---

## 4. Execução da Suíte de Testes Automatizados

Executar os testes unitários e de integração (com Testcontainers):
```bash
cd backend
mvn clean test
```
- **Resultado Esperado**: 100% dos testes unitários e de integração passando sem falhas.
