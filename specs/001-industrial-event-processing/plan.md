# Implementation Plan: Industrial Event Ingestion and Processing

**Branch**: `001-industrial-event-processing` | **Date**: 2026-10-24 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-industrial-event-processing/spec.md`

## Summary

Implementar a solução Full Stack para ingestão, processamento desacoplado, deduplicação (idempotência), auditoria de eventos `QUALITY`, consulta e monitoramento de eventos industriais utilizando Arquitetura Hexagonal (Ports & Adapters) em Java 17 com Spring Boot 3, persistência em PostgreSQL com migrações Flyway e interface reativa em Angular com Angular Material.

## Technical Context

**Language/Version**: Java 17 (Backend) e TypeScript 5+ (Frontend Angular)

**Primary Dependencies**:
- Backend: Spring Boot 3.2+, Spring Web, Spring Data JPA, Flyway, Bean Validation, Lombok, Spring Security, Spring Boot Actuator, Micrometer, Springdoc OpenAPI.
- Frontend: Angular 17+, Angular Material, RxJS.

**Storage**: PostgreSQL 16 (Flyway para migrações versionadas)

**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL)

**Target Platform**: Execução local multi-container via Docker Compose (Portas 4200, 8080, 5432) e execução nativa via terminal (`mvn spring-boot:run`, `npm start`)

**Project Type**: Aplicação Web Full Stack (Backend REST API + Frontend SPA)

**Performance Goals**: Tempo de resposta de ingestão < 1 segundo para 95% das requisições; ciclo completo de cadastro e visualização na UI < 3 segundos

**Constraints**: Tolerância zero para duplicidade de eventos (idempotência lógica + restrição única no banco); auditoria obrigatória exclusiva para eventos `QUALITY`; desacoplamento total do domínio (Hexagonal); endpoints públicos vs protegidos no Actuator

**Scale/Scope**: Módulo local completo com capacidade de evolução arquitetural desacoplada para AWS (ECS Fargate, RDS PostgreSQL, CloudWatch, SQS/EventBridge)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio Constitucional | Requisito do Projeto | Avaliação de Design | Status |
|:---|:---|:---|:---:|
| **I. Arquitetura Hexagonal & Isolamento** | Núcleo de domínio e casos de uso sem dependências de frameworks ou ORM | Entidades em `domain.model`, casos de uso guiados por `ports.in` e dependentes de `ports.out`. Mapeamentos JPA isolados em `adapters.out.persistence` | **PASS** |
| **II. Extensibilidade via Strategy Pattern** | Processamento de eventos aberto para extensão sem alterar código base (OCP) | Interface `EventProcessor` com implementações separadas `ProductionEventProcessor` e `QualityEventProcessor` | **PASS** |
| **III. Idempotência Rigorosa** | Prevenção absoluta de duplicidade com retorno determinístico 409 Conflict | Checagem no Use Case via `EventRepositoryPort.existsByEventId` + `UNIQUE(event_id)` na tabela `events` do PostgreSQL | **PASS** |
| **IV. Auditoria Imutável para QUALITY** | Eventos QUALITY geram AuditLog; eventos PRODUCTION não | Use case e `QualityEventProcessor` acionam `AuditRepositoryPort` exclusivamente para o tipo `QUALITY` | **PASS** |
| **V. Test-First & Pirâmide de Testes** | Cobertura automatizada unitária e de integração mandatória | Testes unitários com JUnit 5/Mockito para Use Cases e Strategies; testes de integração com Testcontainers para PostgreSQL real | **PASS** |
| **VI. Observabilidade & Métricas Nativas** | Métricas de negócio e técnicas com Actuator e Micrometer | Métricas customizadas (`events.processed`, `events.duplicate`, `events.production`, `events.quality`) e segregação de segurança nos endpoints | **PASS** |
| **VII. Frontend Componentizado & Resiliente** | SPA Angular componentizada com feedback visual e tabela reativa | Módulos standalone, Angular Material (`MatTable`, `MatPaginator`, `MatSnackBar`), ordenação decrescente e RxJS | **PASS** |

## Project Structure

### Documentation (this feature)

```text
specs/001-industrial-event-processing/
├── plan.md              # Este plano de implementação
├── research.md          # Decisões técnicas e arquiteturais (Fase 0)
├── data-model.md        # Entidades, DDL Flyway e ciclo de vida (Fase 1)
├── quickstart.md        # Roteiro de validação prática e testes (Fase 1)
├── contracts/           # Especificação de contratos de API
│   └── events-api.yaml  # OpenAPI 3.0 para /api/events
├── checklists/
│   └── requirements.md  # Checklist de qualidade da especificação
└── tasks.md             # Tarefas de implementação (Fase 2 - /speckit-tasks)
```

### Source Code (repository root)

```text
EventHub/
├── backend/
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/eventhub/
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── Event.java
│   │   │   │   │   │   ├── AuditLog.java
│   │   │   │   │   │   └── EventType.java
│   │   │   │   │   └── ports/
│   │   │   │   │       ├── in/
│   │   │   │   │       │   ├── ProcessEventUseCase.java
│   │   │   │   │       │   └── FindEventsUseCase.java
│   │   │   │   │       └── out/
│   │   │   │   │           ├── EventRepositoryPort.java
│   │   │   │   │           ├── AuditRepositoryPort.java
│   │   │   │   │           └── MetricsPort.java
│   │   │   │   ├── application/
│   │   │   │   │   ├── usecase/
│   │   │   │   │   │   ├── ProcessEventService.java
│   │   │   │   │   │   └── FindEventsService.java
│   │   │   │   │   └── strategy/
│   │   │   │   │       ├── EventProcessor.java
│   │   │   │   │       ├── ProductionEventProcessor.java
│   │   │   │   │       └── QualityEventProcessor.java
│   │   │   │   └── infrastructure/
│   │   │   │       ├── adapters/
│   │   │   │       │   ├── in/web/
│   │   │   │       │   │   ├── EventController.java
│   │   │   │       │   │   ├── GlobalExceptionHandler.java
│   │   │   │       │   │   └── dto/
│   │   │   │       │   │       ├── CreateEventRequest.java
│   │   │   │       │   │       ├── EventResponse.java
│   │   │   │       │   │       └── PageResponse.java
│   │   │   │       │   ├── out/persistence/
│   │   │   │       │   │   ├── JpaEventRepositoryAdapter.java
│   │   │   │       │   │   ├── JpaAuditRepositoryAdapter.java
│   │   │   │       │   │   ├── entities/
│   │   │   │       │   │   │   ├── EventJpaEntity.java
│   │   │   │       │   │   │   └── AuditLogJpaEntity.java
│   │   │   │       │   │   ├── repositories/
│   │   │   │       │   │   │   ├── SpringDataEventRepository.java
│   │   │   │       │   │   │   └── SpringDataAuditRepository.java
│   │   │   │       │   │   └── mappers/
│   │   │   │       │   │       ├── EventPersistenceMapper.java
│   │   │   │       │   │       └── AuditPersistenceMapper.java
│   │   │   │       │   └── out/metrics/
│   │   │   │       │       └── MicrometerMetricsAdapter.java
│   │   │   │       └── config/
│   │   │   │           ├── SecurityConfig.java
│   │   │   │           ├── CorsConfig.java
│   │   │   │           └── OpenApiConfig.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/
│   │   │           └── V1__create_events_and_audit_tables.sql
│   │   └── test/
│   │       └── java/com/eventhub/
│   │           ├── unit/
│   │           │   ├── domain/
│   │           │   ├── application/
│   │           │   └── adapters/
│   │           └── integration/
│   │               ├── persistence/
│   │               └── api/
├── frontend/
│   ├── package.json
│   ├── angular.json
│   ├── tsconfig.json
│   └── src/
│       ├── app/
│       │   ├── components/
│       │   │   ├── event-form/
│       │   │   ├── event-list/
│       │   │   └── navbar/
│       │   ├── models/
│       │   │   └── event.model.ts
│       │   ├── services/
│       │   │   └── event.service.ts
│       │   ├── app.component.ts
│       │   ├── app.component.html
│       │   └── app.config.ts
│       ├── styles.scss
│       └── index.html
├── docker-compose.yml
└── README.md
```

**Structure Decision**: Adotada a estrutura padrão de aplicação corporativa Full Stack desacoplada em `backend/` e `frontend/`. O backend segue a organização pura de Arquitetura Hexagonal com pacotes `domain`, `application` e `infrastructure`, garantindo isolamento total de regras de negócio.

## Complexity Tracking

> *Nenhuma violação aos princípios da Constituição foi cometida. Todos os padrões selecionados (Hexagonal Architecture, Strategy Pattern, Testcontainers, Actuator/Micrometer) estão estritamente alinhados à governança do projeto.*

| Padrão Arquitetural | Justificativa | Alternativa Mais Simples Rejeitada Porque |
|---|---|---|
| **Arquitetura Hexagonal (Ports & Adapters)** | Exigência da Constituição e SRS/SDD para isolamento estrito de domínio | CRUD com entidades acopladas ao Spring Data JPA violaria as regras de governança |
| **Strategy Pattern para EventProcessor** | Exigência constitucional para garantir Princípio Aberto/Fechado (OCP) | Switch-case no serviço geraria acoplamento e alteração contínua de código legado |
| **Testcontainers para Testes de Integração** | Garantir execução idêntica do PostgreSQL real e Flyway nos testes | H2 em memória oculta diferenças de dialeto, UUID e constraints concorrentes |
