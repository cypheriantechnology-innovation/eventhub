# EventHub - Sistema de Recebimento e Processamento de Eventos Industriais

Solução Full Stack de alta resiliência e baixa acoplabilidade projetada para ingestão, processamento desacoplado, deduplicação (idempotência), trilha de auditoria e monitoramento de eventos de chão de fábrica.

---

## 1. Visão Geral da Arquitetura

O backend foi concebido sob os preceitos da **Arquitetura Hexagonal (Ports & Adapters)** e dos princípios **SOLID**, assegurando que o núcleo de domínio permaneça estritamente desacoplado de frameworks, bibliotecas externas ou mecanismos de banco de dados.

```text
               ┌────────────────────────────────────────────────────────┐
               │                     Frontend Angular                   │
               │         (Componentes Standalone + Material + RxJS)     │
               └───────────────────────────┬────────────────────────────┘
                                           │ HTTP REST (JSON)
                                           ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ Spring Boot 3 - Arquitetura Hexagonal                                                  │
│                                                                                        │
│  [Inbound Adapters]                                                                    │
│     ├── EventController (REST API: POST/GET /api/events)                               │
│     └── GlobalExceptionHandler (@RestControllerAdvice)                                 │
│                                                                                        │
│  [Inbound Ports]                                                                       │
│     ├── ProcessEventUseCase                                                            │
│     └── FindEventsUseCase                                                              │
│                                                                                        │
│  [Application Core & Use Cases]                                                        │
│     ├── ProcessEventService (Roteamento de Estratégia + Idempotência Lógica)           │
│     ├── FindEventsService (Ordenação e Paginação)                                      │
│     └── Strategy Pattern:                                                              │
│            ├── EventProcessor (Interface OCP)                                          │
│            ├── ProductionEventProcessor (Persistência padrão)                         │
│            └── QualityEventProcessor (Persistência + Trilha de Auditoria)              │
│                                                                                        │
│  [Domain Model (Core Puro)]                                                            │
│     ├── Event (Entidade pura)                                                          │
│     ├── AuditLog (Entidade imutável)                                                   │
│     ├── EventType (PRODUCTION | QUALITY)                                               │
│     └── PageResult<T>                                                                  │
│                                                                                        │
│  [Outbound Ports]                                                                      │
│     ├── EventRepositoryPort                                                            │
│     ├── AuditRepositoryPort                                                            │
│     └── MetricsPort                                                                    │
│                                                                                        │
│  [Outbound Adapters]                                                                   │
│     ├── JpaEventRepositoryAdapter ──────> Spring Data JPA (PostgreSQL)                │
│     ├── JpaAuditRepositoryAdapter ─────> Tabela audit_logs                             │
│     └── MicrometerMetricsAdapter ──────> Spring Boot Actuator & Prometheus             │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Stack Tecnológica

- **Backend**: Java 17+, Spring Boot 3.2.5, Spring Data JPA, Flyway, Bean Validation, Lombok, Spring Security, Spring Boot Actuator, Micrometer, Springdoc OpenAPI (Swagger).
- **Frontend**: Angular 17, TypeScript, Angular Material, RxJS.
- **Banco de Dados**: PostgreSQL 16 (migrações versionadas via Flyway).
- **Testes**: JUnit 5, Mockito, AssertJ, Spring Boot Test, Testcontainers.
- **Orquestração Local**: Docker & Docker Compose.

---

## 3. Decisões Técnicas Principais

1. **Arquitetura Hexagonal & Isolamento Estrito**:
   - Zero dependências de persistência ou Spring no pacote `domain.model`.
   - Comunicação via portas abstratas (`ports.in` e `ports.out`), permitindo troca transparente de adapters.
2. **Strategy Pattern (Princípio Aberto/Fechado - OCP)**:
   - Interface `EventProcessor` com implementações desacopladas (`ProductionEventProcessor` e `QualityEventProcessor`). Novos tipos futuros (ex.: `MAINTENANCE`) são adicionados sem alterar o código existente.
3. **Idempotência Rigorosa em Dois Níveis**:
   - *Nível Lógico*: `ProcessEventService` verifica `eventRepositoryPort.existsByEventId()` antes do processamento.
   - *Nível Físico*: Restrição `UNIQUE(event_id)` na tabela `events` do PostgreSQL, mitigando concorrência de rede com resposta determinística `409 Conflict`.
4. **Auditoria Obrigatória para Eventos Críticos (`QUALITY`)**:
   - Eventos `QUALITY` geram e persistem automaticamente um registro imutável em `audit_logs` via `AuditRepositoryPort`.
   - Eventos `PRODUCTION` são persistidos apenas operacionalmente, sem overhead de auditoria.
5. **Observabilidade Nativa**:
   - Métricas de negócio (`events.processed`, `events.duplicate`, `events.production`, `events.quality`, `events.processing.time`) e de JVM coletadas via Micrometer e expostas no Actuator.
   - Segregação de segurança: health público (`/actuator/health`) e métricas protegidas (`/actuator/metrics`).

---

## 4. Como Executar a Aplicação

### Opção 1: Execução Completa via Docker Compose (Recomendado)

Na raiz do repositório (`c:/EventHub`):

```bash
docker compose up --build -d
```

Acesse os serviços nos seguintes endereços:
- **Interface Web Angular**: [http://localhost:4200](http://localhost:4200)
- **API REST Spring Boot**: [http://localhost:8082/api/events](http://localhost:8082/api/events)
- **Documentação Interativa Swagger UI**: [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)
- **Actuator Health Check**: [http://localhost:8082/actuator/health](http://localhost:8082/actuator/health)
- **Actuator Métricas (Autenticado)**: [http://localhost:8082/actuator/metrics](http://localhost:8082/actuator/metrics) *(Credenciais: `admin` / `admin`)*
- **PostgreSQL**: `localhost:5433` (Database: `eventhub`, User/Pass: `postgres`/`postgres`)

---

### Opção 2: Execução Local Nativa (Terminal)

#### 1. Iniciar Banco de Dados PostgreSQL
```bash
docker run --name eventhub-postgres -e POSTGRES_DB=eventhub -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16-alpine
```

#### 2. Executar o Backend (Spring Boot)
```bash
cd backend
mvn spring-boot:run
```
*O Flyway executará automaticamente as migrações e criará as tabelas `events` e `audit_logs`.*

#### 3. Executar o Frontend (Angular)
```bash
cd frontend
npm install
npm start
```
*Acesse no navegador: `http://localhost:4200`.*

---

## 5. Como Executar a Suíte de Testes Automatizados

No diretório `backend`:

```bash
cd backend
mvn test
```

A suíte cobre:
- **Testes Unitários**:
  - `ProcessEventServiceTest`: Roteamento de estratégia, validação de duplicidade e métricas.
  - `ProductionEventProcessorTest`: Processamento de produção sem auditoria.
  - `QualityEventProcessorTest`: Disparo automático de registro na trilha de auditoria.
  - `FindEventsServiceTest`: Consulta paginada e exceção para ID não encontrado.
  - `MicrometerMetricsAdapterTest`: Incremento de contadores e timers de processamento.
- **Testes de Integração**:
  - `EventCreationIntegrationTest`: Endpoints de ingestão com códigos HTTP 201 e 400.
  - `IdempotencyIntegrationTest`: Resposta determinística 409 Conflict para eventos duplicados.
  - `FindEventsIntegrationTest`: Paginação e ordenação decrescente por timestamp.
  - `ActuatorMetricsIntegrationTest`: Acesso público vs. protegido no Actuator.

---

## 6. Exemplos de Chamadas à API (cURL)

### Ingestão de Evento de Produção
```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "EVT-PRD-1001",
    "eventType": "PRODUCTION",
    "description": "Linha de montagem A1 iniciada com sucesso",
    "timestamp": "2026-05-20T10:00:00"
  }'
```

### Ingestão de Evento de Qualidade (Gera Auditoria)
```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "EVT-QLT-2001",
    "eventType": "QUALITY",
    "description": "Desvio dimensional no lote 44 detectado"
  }'
```

### Consulta Paginada de Eventos
```bash
curl -X GET "http://localhost:8080/api/events?page=0&size=10&sort=desc"
```

---

## 7. Possíveis Melhorias Futuras (Evolução em Nuvem AWS)

Embora o escopo da entrega atual seja estritamente focado em execução local simples e eficiente, a arquitetura foi desenhada para facilitar a evolução contínua para nuvem AWS sem impacto nas regras de negócio:

1. **Computação & Containers**:
   - Deploy do container Spring Boot no **AWS ECS Fargate**, proporcionando escalabilidade horizontal automática sem gestão de servidores.
2. **Banco de Dados Gerenciado**:
   - Migração do PostgreSQL local para o **Amazon RDS PostgreSQL** com Multi-AZ e backups automatizados.
3. **Distribuição & Borda**:
   - Servir o frontend Angular via **Amazon S3** + **Amazon CloudFront** para distribuição global com latência mínima e cache estático.
   - Proteção de entrada via **AWS API Gateway** e **AWS WAF**.
4. **Mensageria & Ingestão Assíncrona de Alta Escala**:
   - Para volumes industriais com milhões de eventos/segundo, introdução de um Inbound Adapter consumindo de **Amazon SQS** ou **Apache Kafka (Amazon MSK)** via `EventIngestionQueueConsumer`.
5. **Observabilidade em Nuvem**:
   - Envio automático de métricas e logs estruturados para o **Amazon CloudWatch** ou integração com Prometheus/Grafana gerenciados.
