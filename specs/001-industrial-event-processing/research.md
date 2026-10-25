# Technical Research: Industrial Event Ingestion and Processing

**Feature**: [001-industrial-event-processing](spec.md) | **Date**: 2026-10-24

## 1. Arquitetura de Aplicação (Hexagonal / Ports and Adapters)

- **Decisão**: Adotar a Arquitetura Hexagonal (Ports & Adapters) segregando estritamente em:
  - `domain.model`: Entidades puras (`Event`, `AuditLog`, `EventType`), livres de anotações JPA ou dependências de framework.
  - `domain.ports.in`: Interfaces de casos de uso (`ProcessEventUseCase`, `FindEventsUseCase`).
  - `domain.ports.out`: Interfaces de persistência e observabilidade (`EventRepositoryPort`, `AuditRepositoryPort`, `MetricsPort`).
  - `application.usecase`: Implementação das regras de negócio orquestrando ports e strategies.
  - `infrastructure.adapters.in.web`: Controladores REST Spring Boot (`EventController`), DTOs de entrada/saída e `GlobalExceptionHandler`.
  - `infrastructure.adapters.out.persistence`: Implementação JPA com entidades de banco (`EventEntity`, `AuditLogEntity`), Spring Data JPA Repositories e mappers.
  - `infrastructure.adapters.out.metrics`: Implementação com Micrometer.
- **Racional**: Garante desacoplamento total do domínio de negócio em relação a bancos de dados, frameworks ou interfaces web, permitindo testabilidade isolada através de mocks e facilidade de migração/evolução futura (ex.: nuvem AWS).
- **Alternativas Consideradas**:
  - *Arquitetura Tradicional em Camadas (Controller -> Service -> Repository)*: Rejeitada por acoplar entidades de negócio a anotações Hibernate/JPA e violar os princípios da Constituição do projeto.
  - *Clean Architecture (Uncle Bob)*: Similar conceitualmente, porém a nomenclatura Hexagonal (Ports and Adapters) foi expressamente exigida no documento de arquitetura [SRS_SDD_Eventos_Hexagonal_Local_AWS_Evolucao.md](../../SRS_SDD_Eventos_Hexagonal_Local_AWS_Evolucao.md).

---

## 2. Padrão de Processamento Extensível (Strategy Pattern)

- **Decisão**: Utilizar o padrão Strategy com a interface `EventProcessor`, implementada por:
  - `ProductionEventProcessor`: Realiza validação específica de produção e registro padrão sem auditoria.
  - `QualityEventProcessor`: Realiza validações de qualidade e aciona a geração imutável de `AuditLog`.
  - Registro dinâmico via injeção de dependência do Spring (Map com chave `EventType` ou List com método `supports(EventType)`).
- **Racional**: Respeita rigorosamente o Princípio Aberto/Fechado (OCP). Novos tipos de evento futuros (ex.: `MAINTENANCE`, `SAFETY`) podem ser adicionados criando uma nova classe de estratégia sem alterar qualquer linha do código existente de despacho ou dos outros processadores.
- **Alternativas Consideradas**:
  - *Switch/Case ou If/Else no Use Case*: Rejeitado por violar OCP e concentrar complexidade ciclomática crescente.

---

## 3. Garantia de Idempotência e Integridade Concorrente

- **Decisão**: Implementar idempotência em duas barreiras defensivas:
  1. *Barreira Lógica (Aplicação)*: `ProcessEventUseCase` consulta `EventRepositoryPort.existsByEventId(eventId)` antes do processamento. Se já existir, lança `DuplicateEventException`, interceptada pelo `@RestControllerAdvice` retornando HTTP `409 CONFLICT`.
  2. *Barreira Física (Banco de Dados)*: Restrição de unicidade `UNIQUE(event_id)` na tabela `events` via PostgreSQL e Flyway. Em cenários de concorrência simultânea (race conditions), qualquer `DataIntegrityViolationException` disparada pelo banco é capturada e traduzida de forma transparente para `409 CONFLICT`.
- **Racional**: Garante integridade absoluta mesmo sob rajadas de submissões simultâneas concorrentes com threads paralelas, impedindo duplicação de dados e reprocessamento indevido.
- **Alternativas Consideradas**:
  - *Somente verificação em memória*: Inadequada para ambientes concorrentes ou reinicializações.
  - *Tabela de locks distribuídos (Redis)*: Desnecessária para a escala de execução local simples (YAGNI).

---

## 4. Persistência & Versionamento de Banco de Dados

- **Decisão**: PostgreSQL 16 com migrações versionadas via Flyway (`V1__create_events_and_audit_tables.sql`).
- **Racional**: PostgreSQL é padrão corporativo de alta confiabilidade transacional (ACID). O Flyway assegura controle de versão reprodutível, rastreabilidade de schema e inicialização automatizada em ambientes de teste e Docker Compose.
- **Alternativas Consideradas**:
  - *H2 / Banco em memória*: Embora rápido para testes simples, não replica fielmente os comportamentos de concorrência, tipos de dados UUID e constraints do PostgreSQL exigidos no teste técnico.
  - *Liquibase*: Desnecessariamente complexo para a estrutura de tabelas necessária; Flyway oferece SQL puro direto.

---

## 5. Observabilidade & Métricas de Negócio

- **Decisão**: Spring Boot Actuator integrado ao Micrometer:
  - Endpoints públicos: `/actuator/health`, `/actuator/info`.
  - Endpoints protegidos: `/actuator/metrics`, `/actuator/prometheus`.
  - Métricas de Negócio:
    - `events.processed.total` (counter, tagged por `type` e `status`)
    - `events.duplicate.total` (counter)
    - `events.production.total` (counter)
    - `events.quality.total` (counter)
    - `events.processing.duration` (timer/histogram)
  - Métricas Técnicas: JVM, conexões JDBC HikariCP, requisições HTTP e CPU.
- **Racional**: Atende diretamente aos diferenciais de observabilidade do teste técnico e prepara a aplicação para futura integração com Prometheus/Grafana ou AWS CloudWatch.
- **Alternativas Consideradas**:
  - *Apenas logs em console*: Insuficiente para análise métrica e monitoramento industrial contínuo.

---

## 6. Frontend: Arquitetura & UX

- **Decisão**: Angular (versão moderna com Standalone Components), Angular Material para componentes de design (tabela, formulário, botões, snackbar de notificação), RxJS para programação reativa e `HttpClient`.
  - Ordenação decrescente padrão (`timestamp DESC`) e suporte a paginação integrada (`MatPaginator`).
  - Tratamento visual explícito para confirmação de cadastro e erro de duplicidade (`409 CONFLICT`).
- **Racional**: Proporciona uma interface profissional, ágil e moderna com componentes limpos, separação clara entre camada de apresentação e serviços HTTP tipados em TypeScript.
- **Alternativas Consideradas**:
  - *HTML puro + Vanilla JS*: Funcional, mas o documento técnico e arquitetural especificou explicitamente Angular + Angular Material.

---

## 7. Estratégia de Testes Automatizados

- **Decisão**:
  - *Testes Unitários (JUnit 5 + Mockito)*: Testar isoladamente `ProcessEventUseCase`, `FindEventsUseCase`, `ProductionEventProcessor`, `QualityEventProcessor`, `EventController` e `GlobalExceptionHandler`.
  - *Testes de Integração (SpringBootTest + Testcontainers)*: Testar os adapters de persistência e fluxo ponta a ponta contra uma instância real de PostgreSQL rodando em container Docker.
- **Racional**: Garante alta cobertura e fidelidade de testes de banco de dados (constraints únicas e Flyway) sem depender de infraestrutura externa pré-configurada.
