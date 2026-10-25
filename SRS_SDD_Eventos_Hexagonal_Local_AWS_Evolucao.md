# SRS / SDD - Sistema de Recebimento e Processamento de Eventos Industriais

## 1. Objetivo

Desenvolver uma solução Full Stack para recebimento, processamento e consulta de eventos industriais, utilizando Java 17, Spring Boot, Angular e PostgreSQL.

A solução deve ser simples de executar localmente, atender aos requisitos do desafio técnico e demonstrar boas práticas de arquitetura, desenvolvimento e observabilidade.

---

# 2. Escopo da Entrega

## Funcionalidades

- Receber eventos via API REST.
- Impedir processamento duplicado.
- Persistir eventos.
- Gerar auditoria para eventos QUALITY.
- Consultar eventos registrados.
- Exibir eventos em tela Angular.
- Expor métricas operacionais e de negócio.

---

# 3. Arquitetura Implementada (Entrega)

## Arquitetura Hexagonal

```text
Angular
    │
    ▼
REST API (Spring Boot)
    │
Inbound Adapter
    │
Use Cases
    │
Domain
    │
Ports
    │
Outbound Adapters
    │
PostgreSQL
```

### Benefícios

- Baixo acoplamento
- Alta testabilidade
- Facilidade de evolução
- Separação clara de responsabilidades
- Aderência aos princípios SOLID

---

# 4. Stack Tecnológica

## Backend

- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA
- PostgreSQL
- Flyway
- Bean Validation
- Lombok
- Spring Security
- Spring Boot Actuator
- Micrometer
- Swagger/OpenAPI

## Frontend

- Angular
- TypeScript
- Angular Material
- RxJS
- HttpClient

## Testes

- JUnit 5
- Mockito
- Testcontainers

## Infraestrutura Local

- Docker
- Docker Compose

---

# 5. Execução Local

## Serviços

| Componente | Porta |
|------------|--------|
| Angular | 4200 |
| Spring Boot | 8080 |
| PostgreSQL | 5432 |
| Swagger | 8080/swagger-ui |
| Actuator | 8080/actuator |

## Inicialização

### Backend

```bash
mvn spring-boot:run
```

ou

```bash
docker compose up -d
```

### Frontend

```bash
npm install
npm start
```

---

# 6. Aplicação dos Princípios SOLID

## SRP

- Controller → HTTP
- Use Cases → regras de negócio
- Repository Adapter → persistência
- Metrics Service → observabilidade
- Audit Service → auditoria

## OCP

Strategy Pattern para processamento dos eventos.

```java
EventProcessor
```

Implementações:

- ProductionEventProcessor
- QualityEventProcessor

## LSP

Implementações dos adapters podem ser substituídas sem impacto no domínio.

## ISP

Interfaces específicas:

```java
EventRepositoryPort
AuditRepositoryPort
```

## DIP

Use Cases dependem apenas das Ports.

---

# 7. Modelo de Domínio

## Event

```java
UUID id;
String eventId;
EventType eventType;
String description;
LocalDateTime timestamp;
```

## AuditLog

```java
UUID id;
String eventId;
String action;
LocalDateTime createdAt;
```

## EventType

```java
PRODUCTION
QUALITY
```

---

# 8. Casos de Uso

## ProcessEventUseCase

Responsável por:

- Validar duplicidade
- Processar evento
- Persistir evento
- Gerar auditoria
- Registrar métricas

## FindEventsUseCase

Responsável por:

- Consultar eventos

---

# 9. Idempotência

## Aplicação

```java
existsByEventId(eventId)
```

## Banco de Dados

```sql
UNIQUE(event_id)
```

Objetivo:

- Evitar reprocessamento
- Garantir consistência
- Mitigar problemas de concorrência

---

# 10. Persistência

Banco de dados:

```text
PostgreSQL
```

Migrações:

```text
Flyway
```

---

# 11. Processamento por Strategy Pattern

```java
EventProcessor
```

Implementações:

```java
ProductionEventProcessor
```

```java
QualityEventProcessor
```

Permite inclusão futura de novos tipos sem alteração das implementações existentes.

---

# 12. API REST

## Criar Evento

```http
POST /api/events
```

## Listar Eventos

```http
GET /api/events
```

## Buscar Evento

```http
GET /api/events/{eventId}
```

---

# 13. Tratamento Global de Exceções

```java
@RestControllerAdvice
```

Retornos:

```http
400 BAD REQUEST
409 CONFLICT
500 INTERNAL SERVER ERROR
```

---

# 14. Observabilidade

## Spring Boot Actuator

Endpoints:

```text
/actuator/health
/actuator/info
/actuator/metrics
/actuator/prometheus
```

## Segurança dos Endpoints

Públicos:

```text
/actuator/health
/actuator/info
```

Protegidos:

```text
/actuator/metrics
/actuator/prometheus
```

## Micrometer

### Métricas de Negócio

- events.processed
- events.duplicate
- events.production
- events.quality
- events.processing.time

### Métricas Técnicas

- JVM
- Heap
- CPU
- Threads
- JDBC
- HTTP Requests

---

# 15. Testes

## Testes Unitários

- PRODUCTION
- QUALITY
- Duplicidade
- Auditoria
- Métricas

## Testes de Integração

- SpringBootTest
- Testcontainers
- PostgreSQL containerizado

---

# 16. Arquitetura Futura (Evolução)

Esta seção NÃO faz parte da implementação da entrega.

## Evolução AWS

```text
CloudFront
    │
API Gateway
    │
ECS Fargate
    │
Spring Boot
    │
RDS PostgreSQL
```

## Observabilidade em Nuvem

```text
Micrometer
    │
CloudWatch
```

ou

```text
Micrometer
    │
Prometheus
    │
Grafana
```

## Mensageria Futura

- Amazon SQS
- Amazon SNS
- Amazon EventBridge
- Apache Kafka

## Segurança Futura

- OAuth2
- JWT
- AWS Cognito

---

# 17. Decisões Arquiteturais

- Arquitetura Hexagonal para desacoplamento entre domínio e infraestrutura.
- SOLID aplicado em toda a estrutura da aplicação.
- Strategy Pattern para tratamento dos tipos de evento.
- Idempotência garantida por validação de negócio e constraint única no banco.
- Observabilidade incorporada através de Actuator e Micrometer.
- PostgreSQL escolhido por aderência ao contexto corporativo.
- AWS tratada como evolução arquitetural e não como parte da entrega.

---

# 18. Resumo Executivo

A solução foi projetada para execução local utilizando Angular, Spring Boot e PostgreSQL. A arquitetura adota Hexagonal Architecture, princípios SOLID, Strategy Pattern, observabilidade com Actuator e Micrometer, testes automatizados e documentação OpenAPI.

Embora o escopo da entrega seja local, a estrutura foi concebida para permitir evolução futura para ambientes AWS sem impacto significativo nas regras de negócio.
