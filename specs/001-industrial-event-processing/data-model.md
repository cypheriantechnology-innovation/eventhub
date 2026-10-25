# Data Model: Industrial Event Ingestion and Processing

**Feature**: [001-industrial-event-processing](spec.md) | **Date**: 2026-10-24

## 1. Domain Entities (Core Domain)

As entidades de domínio representam os conceitos puros de negócio e estão totalmente desvinculadas de frameworks ORM ou anotações JPA.

### Event
- **Descrição**: Representa o evento industrial recebido de equipamentos da fábrica.
- **Campos**:
  - `id`: `UUID` (Identificador interno imutável gerado pelo sistema)
  - `eventId`: `String` (Identificador de negócio fornecido pelo emissor externo, ex.: "EVT-1001", não nulo, não vazio)
  - `eventType`: `EventType` (Enum: `PRODUCTION`, `QUALITY`, não nulo)
  - `description`: `String` (Descrição operacional detalhada, não nula, 1-500 caracteres)
  - `timestamp`: `LocalDateTime` (Data/hora do evento em formato ISO 8601; preenchido com a data/hora corrente se omitido na requisição)
- **Regras de Negócio**:
  - O `eventId` deve ser único em toda a base histórica.
  - Tentativas de cadastro com `eventId` já existente são bloqueadas por regra de idempotência.

### AuditLog
- **Descrição**: Registro imutável de conformidade e auditoria gerado exclusivamente para eventos críticos (tipo `QUALITY`).
- **Campos**:
  - `id`: `UUID` (Identificador interno imutável da entrada de auditoria)
  - `eventId`: `String` (Referência ao identificador do evento auditado)
  - `action`: `String` (Ação auditada, padrão: "QUALITY_EVENT_REGISTERED")
  - `createdAt`: `LocalDateTime` (Data/hora em que a entrada de auditoria foi gerada no sistema)
- **Regras de Negócio**:
  - Uma vez persistido, um registro de auditoria é estritamente imutável (sem suporte a alteração ou exclusão).
  - Nunca é gerado para eventos do tipo `PRODUCTION`.

### EventType (Enum)
- **Valores**:
  - `PRODUCTION`: Eventos operacionais padrão de chão de fábrica (início de turno, contagem de produção, parada operacional programada).
  - `QUALITY`: Eventos com impacto na qualidade do produto (desvio de especificação, reprovação de lote, inspeção concluída).

---

## 2. Schema de Banco de Dados (PostgreSQL / Flyway)

Migração versionada: `backend/src/main/resources/db/migration/V1__create_events_and_audit_tables.sql`

```sql
-- Extensão para geração de UUID no PostgreSQL se necessário
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabela de Eventos Industriais
CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_events_event_id UNIQUE (event_id)
);

-- Índices para otimização de consulta e ordenação
CREATE INDEX idx_events_timestamp_desc ON events (timestamp DESC);
CREATE INDEX idx_events_event_type ON events (event_type);

-- Tabela de Trilha de Auditoria
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índice para busca rápida de auditoria por evento
CREATE INDEX idx_audit_logs_event_id ON audit_logs (event_id);
```

---

## 3. Mapeamento de Entidades JPA (Outbound Adapter)

Localizado em `backend/src/main/java/com/eventhub/infrastructure/adapters/out/persistence/entities/`:

- `EventJpaEntity`:
  - Mapeia a tabela `events`.
  - `@Column(name = "event_id", unique = true, nullable = false)`.
  - Conversores/Mappers bidirecionais entre `EventJpaEntity` e a entidade de domínio `Event`.
- `AuditLogJpaEntity`:
  - Mapeia a tabela `audit_logs`.
  - Conversores/Mappers entre `AuditLogJpaEntity` e `AuditLog`.

---

## 4. Ciclo de Vida e Transições de Estado

```text
[Recepção do Payload]
        │
        ▼
[Validação Sintática (Bean Validation)] ──(Inválido)──> Retorna 400 Bad Request
        │ (Válido)
        ▼
[Preenchimento de Fallback (Timestamp se ausente)]
        │
        ▼
[Verificação de Idempotência (existsByEventId)] ──(Existe)──> Retorna 409 Conflict
        │ (Inédito)
        ▼
[Roteamento Strategy (EventProcessor)]
        │
   ┌────┴──────────────────────────┐
   ▼                               ▼
[PRODUCTION]                  [QUALITY]
Persiste Evento               1. Persiste Evento
                              2. Gera e Persiste AuditLog
   │                               │
   └───────────────┬───────────────┘
                   ▼
         [Incrementa Métricas]
                   ▼
         [Retorna 201 Created]
```
