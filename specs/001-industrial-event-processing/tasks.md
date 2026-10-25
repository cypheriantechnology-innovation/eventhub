# Tasks: Industrial Event Ingestion and Processing

**Branch**: `001-industrial-event-processing` | **Date**: 2026-10-24 | **Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

Este documento define todas as tarefas de engenharia necessárias para a implementação completa e testável da funcionalidade de processamento de eventos industriais, organizadas por fases e histórias de usuário.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialização da infraestrutura do projeto, gerenciamento de dependências e orquestração de contêineres.

- [X] T001 Initialize Spring Boot Maven backend project structure with Java 17, Spring Boot 3, Flyway, PostgreSQL, Actuator, Micrometer and Lombok dependencies in backend/pom.xml
- [X] T002 [P] Initialize Angular SPA frontend project structure with TypeScript, Angular Material and RxJS dependencies in frontend/package.json
- [X] T003 [P] Configure multi-container orchestration with PostgreSQL 16 (port 5432), backend (port 8080) and frontend (port 4200) in docker-compose.yml
- [X] T004 [P] Configure application properties, PostgreSQL DataSource, Flyway migration settings, and logging levels in backend/src/main/resources/application.yml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infraestrutura central de domínio, persistência, migrações de banco e tratamento global de erros.

⚠️ **CRITICAL**: Nenhuma tarefa das histórias de usuário pode iniciar antes da conclusão desta fundação.

- [X] T005 [P] Create Flyway migration script V1__create_events_and_audit_tables.sql creating table `events` with columns `id UUID PRIMARY KEY`, `event_id VARCHAR(100) NOT NULL`, `event_type VARCHAR(50) NOT NULL`, `description VARCHAR(500) NOT NULL`, `timestamp TIMESTAMP NOT NULL`, `created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`, `CONSTRAINT uk_events_event_id UNIQUE (event_id)`, and table `audit_logs` in backend/src/main/resources/db/migration/V1__create_events_and_audit_tables.sql
- [X] T006 [P] Create domain model EventType enum with values `PRODUCTION` and `QUALITY` in backend/src/main/java/com/eventhub/domain/model/EventType.java
- [X] T007 [P] Create domain entity Event (id UUID, eventId String, eventType EventType, description String, timestamp LocalDateTime) without framework annotations in backend/src/main/java/com/eventhub/domain/model/Event.java
- [X] T008 [P] Create domain entity AuditLog (id UUID, eventId String, action String, createdAt LocalDateTime) without framework annotations in backend/src/main/java/com/eventhub/domain/model/AuditLog.java
- [X] T009 [P] Create EventRepositoryPort outbound port interface with methods `save(Event)`, `findByEventId(String)`, `existsByEventId(String)` and `findAll(Pageable)` in backend/src/main/java/com/eventhub/domain/ports/out/EventRepositoryPort.java
- [X] T010 [P] Create AuditRepositoryPort outbound port interface with method `save(AuditLog)` in backend/src/main/java/com/eventhub/domain/ports/out/AuditRepositoryPort.java
- [X] T011 [P] Create MetricsPort outbound port interface with methods `incrementProcessed(EventType)`, `incrementDuplicate()`, and `recordProcessingTime(EventType, Duration)` in backend/src/main/java/com/eventhub/domain/ports/out/MetricsPort.java
- [X] T012 [P] Create EventJpaEntity mapping `events` table with `@Column(name = "event_id", unique = true, nullable = false, length = 100)` in backend/src/main/java/com/eventhub/infrastructure/adapters/out/persistence/entities/EventJpaEntity.java
- [X] T013 [P] Create AuditLogJpaEntity mapping `audit_logs` table in backend/src/main/java/com/eventhub/infrastructure/adapters/out/persistence/entities/AuditLogJpaEntity.java
- [X] T014 [P] Create SpringDataEventRepository interface extending JpaRepository in backend/src/main/java/com/eventhub/infrastructure/adapters/out/persistence/repositories/SpringDataEventRepository.java
- [X] T015 [P] Create SpringDataAuditRepository interface extending JpaRepository in backend/src/main/java/com/eventhub/infrastructure/adapters/out/persistence/repositories/SpringDataAuditRepository.java
- [X] T016 [P] Create EventPersistenceMapper and AuditPersistenceMapper to translate between domain entities and JPA entities in backend/src/main/java/com/eventhub/infrastructure/adapters/out/persistence/mappers/
- [X] T017 Implement JpaEventRepositoryAdapter implementing EventRepositoryPort in backend/src/main/java/com/eventhub/infrastructure/adapters/out/persistence/JpaEventRepositoryAdapter.java
- [X] T018 [P] Implement JpaAuditRepositoryAdapter implementing AuditRepositoryPort in backend/src/main/java/com/eventhub/infrastructure/adapters/out/persistence/JpaAuditRepositoryAdapter.java
- [X] T019 [P] Configure Spring Security permitting unrestricted access to `/api/events/**`, `/swagger-ui/**`, `/v3/api-docs/**`, and `/actuator/health`, while protecting `/actuator/metrics/**` and `/actuator/prometheus` with basic auth in backend/src/main/java/com/eventhub/infrastructure/config/SecurityConfig.java
- [X] T020 [P] Configure CORS allowing requests from `http://localhost:4200` with methods GET, POST, OPTIONS in backend/src/main/java/com/eventhub/infrastructure/config/CorsConfig.java
- [X] T021 [P] Configure OpenAPI 3.0 documentation metadata in backend/src/main/java/com/eventhub/infrastructure/config/OpenApiConfig.java
- [X] T022 Implement GlobalExceptionHandler with `@RestControllerAdvice` handling `MethodArgumentNotValidException` (400), `DuplicateEventException` (409), `DataIntegrityViolationException` (409), and generic `Exception` (500) in backend/src/main/java/com/eventhub/infrastructure/adapters/in/web/GlobalExceptionHandler.java

**Checkpoint**: Fundação pronta — implementação das histórias de usuário pode começar.

---

## Phase 3: User Story 1 - Recebimento e Registro de Eventos de Produção (Priority: P1) 🎯 MVP

**Goal**: Permitir que equipamentos ou operadores submetam eventos industriais do tipo `PRODUCTION`, validem sintaticamente o payload, preencham fallback de timestamp se ausente, persistam o evento e retornem confirmação com código HTTP `201 Created`.

**Independent Test**: Submeter payload válido de evento `PRODUCTION` via `POST /api/events` e verificar retorno `201 Created` com o evento salvo na base de dados.

### Tests for User Story 1

- [X] T023 [P] [US1] Unit test for ProductionEventProcessor validating production event processing without audit logging in backend/src/test/java/com/eventhub/unit/application/ProductionEventProcessorTest.java
- [X] T024 [P] [US1] Unit test for ProcessEventService validating execution flow, timestamp fallback when omitted, and persistence in backend/src/test/java/com/eventhub/unit/application/ProcessEventServiceTest.java
- [X] T025 [P] [US1] Integration test for event creation endpoint POST /api/events returning 201 Created and persisted entity in backend/src/test/java/com/eventhub/integration/api/EventCreationIntegrationTest.java

### Implementation for User Story 1

- [X] T026 [P] [US1] Create ProcessEventUseCase inbound port interface with method `process(Event)` in backend/src/main/java/com/eventhub/domain/ports/in/ProcessEventUseCase.java
- [X] T027 [P] [US1] Create EventProcessor strategy interface with methods `supports(EventType)` and `process(Event)` in backend/src/main/java/com/eventhub/application/strategy/EventProcessor.java
- [X] T028 [US1] Implement ProductionEventProcessor for `PRODUCTION` event type in backend/src/main/java/com/eventhub/application/strategy/ProductionEventProcessor.java
- [X] T029 [US1] Implement ProcessEventService routing to matching strategy, defaulting timestamp to `LocalDateTime.now()` if null, and saving via EventRepositoryPort in backend/src/main/java/com/eventhub/application/usecase/ProcessEventService.java
- [X] T030 [P] [US1] Create CreateEventRequest DTO with Bean Validation annotations (`@NotBlank eventId`, `@NotNull eventType`, `@NotBlank @Size(max=500) description`, optional `timestamp`) in backend/src/main/java/com/eventhub/infrastructure/adapters/in/web/dto/CreateEventRequest.java
- [X] T031 [P] [US1] Create EventResponse DTO in backend/src/main/java/com/eventhub/infrastructure/adapters/in/web/dto/EventResponse.java
- [X] T032 [US1] Implement POST /api/events endpoint in EventController in backend/src/main/java/com/eventhub/infrastructure/adapters/in/web/EventController.java

**Checkpoint**: User Story 1 totalmente funcional e testável de forma independente (MVP alcançado).

---

## Phase 4: User Story 2 - Idempotência e Prevenção de Duplicidade (Priority: P1)

**Goal**: Garantir que múltiplos envios de um mesmo `eventId` sejam rejeitados deterministicamente com código HTTP `409 Conflict`, preservando o histórico de dados intacto e sem reprocessamento.

**Independent Test**: Submeter o mesmo `eventId` duas vezes consecutivas; a primeira deve retornar `201 Created` e a segunda deve retornar `409 Conflict` com mensagem descritiva.

### Tests for User Story 2

- [X] T033 [P] [US2] Unit test for ProcessEventService duplicate detection throwing DuplicateEventException when existsByEventId is true in backend/src/test/java/com/eventhub/unit/application/DuplicateEventTest.java
- [X] T034 [P] [US2] Integration test with Testcontainers submitting concurrent requests with identical eventId and verifying exactly one succeeds and the other receives 409 Conflict in backend/src/test/java/com/eventhub/integration/api/IdempotencyIntegrationTest.java

### Implementation for User Story 2

- [X] T035 [US2] Create DuplicateEventException in backend/src/main/java/com/eventhub/domain/exception/DuplicateEventException.java
- [X] T036 [US2] Add idempotency validation in ProcessEventService calling `eventRepositoryPort.existsByEventId(event.getEventId())` before processing, and map DuplicateEventException to HTTP 409 in backend/src/main/java/com/eventhub/infrastructure/adapters/in/web/GlobalExceptionHandler.java

**Checkpoint**: Idempotência garantida tanto em nível de aplicação quanto em nível de banco de dados.

---

## Phase 5: User Story 3 - Auditoria Automática para Eventos de Qualidade (Priority: P2)

**Goal**: Garantir que eventos do tipo `QUALITY` gerem automaticamente um registro de auditoria imutável (`AuditLog`) contendo `eventId`, ação e timestamp via `AuditRepositoryPort`.

**Independent Test**: Submeter evento com `eventType: QUALITY` e verificar que tanto o evento quanto o registro de auditoria na tabela `audit_logs` foram gravados.

### Tests for User Story 3

- [X] T037 [P] [US3] Unit test for QualityEventProcessor verifying that AuditLog is created and persisted via AuditRepositoryPort in backend/src/test/java/com/eventhub/unit/application/QualityEventProcessorTest.java
- [X] T038 [P] [US3] Integration test verifying database state has both event and audit log after POST of QUALITY event in backend/src/test/java/com/eventhub/unit/application/QualityEventProcessorTest.java

### Implementation for User Story 3

- [X] T039 [US3] Implement QualityEventProcessor for `QUALITY` event type generating and saving `AuditLog` with action "QUALITY_EVENT_REGISTERED" in backend/src/main/java/com/eventhub/application/strategy/QualityEventProcessor.java
- [X] T040 [US3] Register QualityEventProcessor in Spring context and integrate into ProcessEventService strategy registry in backend/src/main/java/com/eventhub/application/usecase/ProcessEventService.java

**Checkpoint**: Eventos de qualidade devidamente auditados com isolamento estrito de eventos de produção.

---

## Phase 6: User Story 4 - Consulta e Visualização em Painel Web (Priority: P2)

**Goal**: Fornecer endpoints de consulta paginada (`GET /api/events` ordenados decrescente por timestamp) e interface gráfica Angular com formulário de cadastro, tabela de eventos e mensagens de feedback.

**Independent Test**: Acessar a interface web no navegador, cadastrar um novo evento, visualizar o novo item no topo da tabela e verificar mensagem de erro ao reenviar o mesmo ID.

### Tests for User Story 4

- [X] T041 [P] [US4] Unit test for FindEventsService verifying pageable query and sorting logic in backend/src/test/java/com/eventhub/unit/application/FindEventsServiceTest.java
- [X] T042 [P] [US4] Integration test for GET /api/events with pagination parameters (page, size) returning events ordered by timestamp DESC in backend/src/test/java/com/eventhub/integration/api/FindEventsIntegrationTest.java

### Implementation for User Story 4

- [X] T043 [P] [US4] Create FindEventsUseCase inbound port interface with methods `findAll(Pageable)` and `findByEventId(String)` in backend/src/main/java/com/eventhub/domain/ports/in/FindEventsUseCase.java
- [X] T044 [P] [US4] Create PageResponse DTO for paginated event responses in backend/src/main/java/com/eventhub/infrastructure/adapters/in/web/dto/PageResponse.java
- [X] T045 [US4] Implement FindEventsService querying EventRepositoryPort with default timestamp DESC sorting in backend/src/main/java/com/eventhub/application/usecase/FindEventsService.java
- [X] T046 [US4] Implement GET /api/events (paginated) and GET /api/events/{eventId} in EventController in backend/src/main/java/com/eventhub/infrastructure/adapters/in/web/EventController.java
- [X] T047 [P] [US4] Create TypeScript Event model and EventRequest interfaces in frontend/src/app/models/event.model.ts
- [X] T048 [P] [US4] Implement EventService with methods `createEvent`, `getEvents` and `getEventById` using HttpClient in frontend/src/app/services/event.service.ts
- [X] T049 [P] [US4] Implement EventFormComponent with ReactiveFormsModule, validation indicators and MatSnackBar feedback in frontend/src/app/components/event-form/event-form.component.ts and event-form.component.html
- [X] T050 [P] [US4] Implement EventListComponent with MatTable, MatPaginator and status chips in frontend/src/app/components/event-list/event-list.component.ts and event-list.component.html
- [X] T051 [US4] Implement NavbarComponent and assemble main dashboard view in frontend/src/app/app.component.ts and app.component.html

**Checkpoint**: Interface web completa, responsiva e integrada à API com consulta e cadastro.

---

## Phase 7: User Story 5 - Monitoramento Operacional e Indicadores de Negócio (Priority: P3)

**Goal**: Expor métricas técnicas e de negócio (`events.processed`, `events.duplicate`, `events.production`, `events.quality`) via Micrometer e Actuator.

**Independent Test**: Submeter eventos normais e duplicados e verificar os contadores em `/actuator/metrics/events.processed` e `/actuator/metrics/events.duplicate`.

### Tests for User Story 5

- [X] T052 [P] [US5] Unit test for MicrometerMetricsAdapter verifying meter increments and timing measurements in backend/src/test/java/com/eventhub/unit/adapters/MicrometerMetricsAdapterTest.java
- [X] T053 [P] [US5] Integration test for /actuator/metrics verifying authentication protection on metrics endpoints in backend/src/test/java/com/eventhub/integration/api/ActuatorMetricsIntegrationTest.java

### Implementation for User Story 5

- [X] T054 [US5] Implement MicrometerMetricsAdapter implementing MetricsPort with meters `events.processed`, `events.duplicate`, `events.production`, `events.quality`, and `events.processing.time` in backend/src/main/java/com/eventhub/infrastructure/adapters/out/metrics/MicrometerMetricsAdapter.java
- [X] T055 [US5] Instrument ProcessEventService to call MetricsPort on successful ingestion, duplicate rejection, and per event type in backend/src/main/java/com/eventhub/application/usecase/ProcessEventService.java
- [X] T056 [US5] Configure Actuator endpoints exposure, health probes, and basic auth credentials in backend/src/main/java/com/eventhub/infrastructure/config/SecurityConfig.java

**Checkpoint**: Observabilidade de negócio e técnica plenamente operacional.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Documentação completa da solução, validação cruzada e empacotamento final.

- [X] T057 [P] Create comprehensive README.md with system overview, architecture diagram, step-by-step local execution (`mvn`, `npm`, Docker), test execution, decisions taken, and future AWS evolution in README.md
- [X] T058 Validate all verification scenarios documented in quickstart.md using automated test suite and curl scripts in specs/001-industrial-event-processing/quickstart.md
- [X] T059 Review code cleanliness, verify Hexagonal boundaries (zero external imports in domain core), remove unused dependencies and verify build in backend/ and frontend/

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem dependências — pode iniciar imediatamente.
- **Foundational (Phase 2)**: Depende da conclusão do Setup — BLOQUEIA todas as histórias de usuário.
- **User Stories (Phases 3 a 7)**:
  - Todas dependem da conclusão da Fase 2 (Foundational).
  - US1 (P1) e US2 (P1) são os pilares centrais de ingestão e idempotência.
  - US3 (P2) expande US1 com a estratégia de qualidade e auditoria.
  - US4 (P2) consome os dados persistidos por US1/US2/US3 para exibição e interação web.
  - US5 (P3) adiciona instrumentação de métricas transversalmente às operações de US1, US2 e US3.
- **Polish (Phase 8)**: Depende da conclusão de todas as histórias desejadas.

### Parallel Opportunities

- Todas as tarefas marcadas com `[P]` na Fase 1 e Fase 2 podem ser executadas em paralelo.
- Na Fase 3 (US1), os testes T023, T024 e T025 podem ser escritos em paralelo; os DTOs T030 e T031 podem ser criados em paralelo com a porta T026.
- Na Fase 6 (US4), os componentes de frontend (T047, T048, T049, T050) podem ser implementados em paralelo com as rotas de backend (T043, T044, T045, T046).

---

## Implementation Strategy: MVP First

1. **Passo 1**: Executar Phase 1 (Setup) e Phase 2 (Foundational).
2. **Passo 2**: Executar Phase 3 (User Story 1 - Ingestão de Produção).
3. **Validação do MVP**: Testar `POST /api/events` de forma isolada comprovando persistência e resposta `201 Created`.
4. **Passo 3**: Adicionar Phase 4 (User Story 2 - Idempotência).
5. **Passo 4**: Adicionar Phase 5 (User Story 3 - Auditoria de Qualidade).
6. **Passo 5**: Adicionar Phase 6 (User Story 4 - Interface Web e Consulta).
7. **Passo 6**: Adicionar Phase 7 (User Story 5 - Observabilidade e Métricas).
8. **Passo 7**: Concluir Phase 8 (README, Validação contra quickstart.md e Polish).
