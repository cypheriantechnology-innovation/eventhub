<!--
Sync Impact Report:
- Version change: 0.0.0 (template) → 1.0.0
- List of modified principles:
  - [PRINCIPLE_1_NAME] → I. Arquitetura Hexagonal & Isolamento de Domínio (Ports and Adapters)
  - [PRINCIPLE_2_NAME] → II. Extensibilidade por Estratégia (Open-Closed Principle)
  - [PRINCIPLE_3_NAME] → III. Idempotência Rigorosa & Consistência de Dados
  - [PRINCIPLE_4_NAME] → IV. Auditoria Obrigatória e Imutável para Eventos de Qualidade (QUALITY)
  - [PRINCIPLE_5_NAME] → V. Test-First & Pirâmide de Testes Automatizados (NON-NEGOTIABLE)
  - Added Principle → VI. Observabilidade e Métricas de Negócio Nativas
  - Added Principle → VII. Frontend Componentizado, Reativo e Resiliente
- Added sections:
  - Stack Tecnológica & Restrições de Execução
  - Padrões de API, Tratamento de Erros & Quality Gates
- Removed sections: None
- Follow-up TODOs: None
-->

# EventHub Constitution

## Core Principles

### I. Arquitetura Hexagonal & Isolamento de Domínio (Ports and Adapters)
O núcleo de negócio e as entidades de domínio MUST permanecer completamente isolados de frameworks, bibliotecas externas, componentes de UI e mecanismos de banco de dados. Todas as interações externas MUST transitar através de portas explícitas (interfaces de Inbound Ports e Outbound Ports).
- Os Use Cases controlam o fluxo de negócio e dependem exclusivamente de Ports abstratas.
- Os Inbound Adapters (ex.: REST Controllers Spring Boot) traduzem comandos externos para chamadas aos Use Cases.
- Os Outbound Adapters (ex.: Repositórios JPA PostgreSQL, serviços de auditoria e métricas) implementam as Ports do domínio.
- Nenhuma anotação de persistência ou dependência de framework pode invadir o núcleo de domínio.

### II. Extensibilidade por Estratégia (Open-Closed Principle)
O processamento dos diferentes tipos de eventos industriais MUST ser desacoplado através do Strategy Pattern (`EventProcessor`).
- A adição de novos tipos de eventos MUST ser realizada exclusivamente mediante o registro de uma nova implementação de `EventProcessor`, sem necessidade de alterar as implementações existentes ou o mecanismo central de despacho (Aberto para extensão, fechado para modificação).
- As especializações (ex.: `ProductionEventProcessor`, `QualityEventProcessor`) devem isolar as particularidades e regras de ciclo de vida de cada categoria de evento.

### III. Idempotência Rigorosa & Consistência de Dados
Cada evento recebido MUST conter um identificador único de negócio (`eventId`). O sistema MUST assegurar a idempotência através de duas camadas de defesa obrigatórias:
1. Validação em nível de aplicação no Use Case consultando a porta de repositório antes da execução do processamento.
2. Imposição de restrição de unicidade no banco de dados (`UNIQUE(event_id)`).
- Eventos com identificadores já registrados MUST ser rejeitados deterministicamente com código HTTP `409 CONFLICT`, sem reprocessamento, sem efeitos colaterais e sem corrupção do histórico de dados.

### IV. Auditoria Obrigatória e Imutável para Eventos de Qualidade (QUALITY)
Eventos classificados como `QUALITY` MUST acionar automaticamente a criação e persistência de um registro de auditoria imutável (`AuditLog`), contendo `eventId`, ação realizada e timestamp, através de uma porta dedicada (`AuditRepositoryPort`).
- Eventos do tipo `PRODUCTION` MUST apenas ser persistidos e registrados operacionalmente, sem a criação de logs de auditoria.
- A auditoria não pode interferir no isolamento nem contaminar os dados cadastrais dos eventos operacionais.

### V. Test-First & Pirâmide de Testes Automatizados (NON-NEGOTIABLE)
A cobertura por testes automatizados é mandatória e inegociável em todas as camadas da aplicação:
- Testes unitários (JUnit 5 + Mockito) MUST cobrir as regras de negócio dos Use Cases, o isolamento dos processadores de estratégia, as rotas de duplicidade, os gatilhos de auditoria e a emissão de métricas.
- Testes de integração (SpringBootTest + Testcontainers) MUST validar a comunicação real com PostgreSQL conteinerizado, garantindo o correto funcionamento das migrações Flyway e transações de banco.
- Código sem testes automatizados correspondentes MUST ser sumariamente rejeitado.

### VI. Observabilidade e Métricas de Negócio Nativas
A aplicação MUST nascer instrumentada e monitorável tanto sob a perspectiva técnica quanto de negócio:
- Exposição padronizada via Spring Boot Actuator, separando endpoints públicos (`/actuator/health`, `/actuator/info`) de endpoints protegidos (`/actuator/metrics`, `/actuator/prometheus`).
- Coleta de métricas via Micrometer com indicadores de negócio (`events.processed`, `events.duplicate`, `events.production`, `events.quality`, `events.processing.time`) e indicadores de infraestrutura (JVM, memória, CPU, JDBC e tempo de resposta HTTP).
- Logs estruturados e contextuais para rastreabilidade de falhas e diagnósticos rápidos.

### VII. Frontend Componentizado, Reativo e Resiliente
A interface web (Angular + TypeScript) MUST adotar arquitetura de componentes bem segregada, reatividade baseada em RxJS e estilização com Angular Material:
- Separação clara entre componentes de apresentação e serviços de comunicação (`HttpClient`).
- Telas obrigatórias de cadastro com validação de campos (`eventId`, `eventType`, `description`) e visualização tabular com filtros e status dos eventos processados.
- Notificação clara ao usuário para operações bem-sucedidas e tratamento amigável de erros (incluindo tratamento visual explícito de duplicidade `409 CONFLICT`).

## Stack Tecnológica & Restrições de Execução

- **Backend**: Java 17, Spring Boot 3, Spring Web, Spring Data JPA, PostgreSQL, Flyway, Bean Validation, Lombok, Spring Security, Spring Boot Actuator, Micrometer, Swagger / OpenAPI.
- **Frontend**: Angular, TypeScript, Angular Material, RxJS, HttpClient.
- **Testes**: JUnit 5, Mockito, Testcontainers, PostgreSQL.
- **Execução Local & Containers**:
  - Docker e Docker Compose configurados para orquestração local rápida (Angular na porta 4200, Spring Boot na porta 8080, PostgreSQL na porta 5432).
  - Execução direta via terminal local (`mvn spring-boot:run` e `npm start`) suportada nativamente.
- **Fronteira Arquitetural de Evolução**:
  - A arquitetura da entrega foca na execução local simples, robusta e modular.
  - Padrões para evolução em nuvem AWS (ECS Fargate, RDS PostgreSQL, CloudFront, API Gateway, SQS/SNS/Kafka e AWS Cognito) são mapeados como direcionamento arquitetural futuro e NÃO devem adicionar sobrecarga operacional ou custos prematuros à entrega local atual (princípio YAGNI na entrega, clareza na evolução).

## Padrões de API, Tratamento de Erros & Quality Gates

- **Contrato REST**:
  - `POST /api/events`: Criação e processamento de evento industrial.
  - `GET /api/events`: Listagem dos eventos registrados.
  - `GET /api/events/{eventId}`: Consulta detalhada de evento por ID único.
- **Tratamento Global de Exceções**:
  - Implementação mandatória de `@RestControllerAdvice` para interceptação e padronização de respostas de erro:
    - `400 BAD REQUEST`: Erros de validação estrutural ou payload malformado.
    - `409 CONFLICT`: Violação de idempotência / tentativa de reprocessamento de `eventId` existente.
    - `500 INTERNAL SERVER ERROR`: Falhas internas não tratadas, sem vazamento de stack traces ou dados sensíveis.
- **Documentação de Contrato**:
  - Especificação e interface interativa via Swagger / OpenAPI (`/swagger-ui`) mantidas sincronizadas com os endpoints.
- **Critérios de Qualidade e Aceite (Quality Gates)**:
  - 100% dos testes unitários e de integração passando na suíte automatizada.
  - Verificação estrita de acoplamento da Arquitetura Hexagonal (Zero dependências externas no Domain Core).
  - Documentação README obrigatória contendo instruções de execução (local e Docker), execução de testes, decisões técnicas tomadas e justificativa arquitetural.

## Governance

- Esta Constituição tem prevalência sobre quaisquer decisões pontuais ou conveniências temporárias de implementação no projeto EventHub.
- Qualquer alteração nos princípios estabelecidos ou inclusão de novos padrões arquiteturais exige uma emenda constitucional formal, registro de justificativa técnica e incremento da versão semântica (SemVer).
- As revisões de código (Pull Requests) e as análises automatizadas de qualidade MUST verificar a conformidade com as regras deste documento antes de aprovar qualquer integração.

**Version**: 1.0.0 | **Ratified**: 2026-10-24 | **Last Amended**: 2026-10-24
