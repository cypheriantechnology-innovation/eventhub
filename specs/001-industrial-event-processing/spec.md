# Feature Specification: Industrial Event Ingestion and Processing

**Feature Branch**: `001-industrial-event-processing`

**Created**: 2026-10-24

**Status**: Draft

**Input**: User description: "SRS / SDD - Sistema de Recebimento e Processamento de Eventos Industriais @[SRS_SDD_Eventos_Hexagonal_Local_AWS_Evolucao.md]"

## Clarifications

### Session 2026-10-24
- Q: Como deve ser configurado o controle de autenticação e acesso para a API REST e a interface web na entrega local? → A: Acesso aberto para a API (`/api/events`) e interface Angular com CORS habilitado para execução local simplificada; proteção de segurança aplicada apenas aos endpoints de métricas do Actuator (`/actuator/metrics`, `/actuator/prometheus`).
- Q: Como os eventos devem ser ordenados e paginados por padrão na consulta da API e na tabela da interface web? → A: Ordenação decrescente por timestamp (mais recentes primeiro) com suporte a paginação simples tanto no backend quanto no frontend.
- Q: Como o sistema deve se comportar se o campo de data/hora (timestamp) não for enviado no payload do evento? → A: Timestamp opcional na entrada: se não for fornecido ou vier nulo, o sistema preenche automaticamente com a data/hora corrente de recepção no servidor.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Recebimento e Registro de Eventos de Produção (Priority: P1)

Equipamentos industriais e operadores fabris precisam enviar eventos de chão de fábrica (como início ou parada de linha de produção) para o sistema central. O sistema deve validar os dados recebidos, confirmar o registro do evento e armazená-lo com sucesso, permitindo o acompanhamento em tempo real das atividades produtivas.

**Why this priority**: É a funcionalidade central e o valor primário do sistema. Sem a capacidade de receber e registrar eventos, nenhuma outra função (consulta, auditoria, métricas) pode operar.

**Independent Test**: Pode ser testado de forma isolada enviando um evento válido do tipo `PRODUCTION` com identificador único, descrição e timestamp, verificando se o sistema confirma o registro e se o evento passa a constar na base de dados.

**Acceptance Scenarios**:

1. **Given** um evento de produção válido com identificador único "EVT-1001", tipo "PRODUCTION", timestamp e descrição da operação, **When** o emissor submete o evento para ingestão, **Then** o sistema valida e registra o evento com sucesso, retornando confirmação imediata de criação com os dados registrados.
2. **Given** um evento submetido com campos obrigatórios ausentes ou em branco (como identificador vazio ou tipo inválido), **When** o emissor tenta registrá-lo, **Then** o sistema rejeita a operação com mensagem de erro explicativa indicando os campos inválidos.

---

### User Story 2 - Idempotência e Prevenção de Duplicidade (Priority: P1)

Equipamentos fabris e redes industriais frequentemente realizam retransmissões automáticas devido a oscilações de rede. O sistema deve garantir que múltiplos envios de um mesmo evento (com o mesmo identificador único) nunca resultem em reprocessamento ou duplicação de dados, protegendo a integridade histórica.

**Why this priority**: Evitar duplicidade é crítico em ambientes industriais para impedir contagens duplicadas de peças, alertas falsos ou inconsistências na linha de produção.

**Independent Test**: Pode ser testado submetendo o mesmo identificador de evento duas vezes consecutivas; o primeiro envio deve ser aceito e o segundo deve ser rejeitado com status explícito de conflito e mensagem de duplicidade.

**Acceptance Scenarios**:

1. **Given** que um evento com identificador "EVT-1001" já foi processado e registrado com sucesso, **When** qualquer cliente ou equipamento tenta submeter novamente um evento com o identificador "EVT-1001", **Then** o sistema bloqueia o registro, preserva o estado original sem alterações e retorna uma resposta indicando conflito de duplicidade.
2. **Given** requisições simultâneas concorrentes contendo o mesmo identificador de evento, **When** ambas chegam ao sistema, **Then** exatamente uma é aceita e registrada, enquanto a outra é rejeitada por duplicidade.

---

### User Story 3 - Auditoria Automática para Eventos de Qualidade (Priority: P2)

Setores de controle de qualidade precisam de rastreabilidade formal para eventos que afetam os padrões dos produtos manufaturados. Ao receber um evento do tipo `QUALITY`, o sistema deve não apenas registrá-lo, mas também gerar um registro de auditoria imutável associado, documentando a ação executada e a data/hora exata do registro.

**Why this priority**: Garante conformidade com normas regulatórias e de qualidade industrial (ISO/auditoria de processo), diferenciando eventos comuns de eventos que demandam rastreabilidade auditável.

**Independent Test**: Submeter um evento do tipo `QUALITY` e verificar se tanto o registro do evento quanto o registro correspondente na trilha de auditoria foram gerados. Submeter em seguida um evento do tipo `PRODUCTION` e verificar que nenhum log de auditoria foi gerado.

**Acceptance Scenarios**:

1. **Given** um evento de qualidade válido com identificador "EVT-2001" e tipo "QUALITY", **When** o evento é processado pelo sistema, **Then** o evento é persistido e um registro de auditoria correspondente é gerado automaticamente, contendo o identificador do evento, a ação de auditoria executada e o timestamp de registro.
2. **Given** um evento do tipo "PRODUCTION", **When** o evento é processado, **Then** o evento é registrado normalmente e nenhum registro de auditoria é criado.

---

### User Story 4 - Consulta e Visualização em Painel Web (Priority: P2)

Supervisores de fábrica e operadores precisam de uma interface web simples e intuitiva para consultar os eventos registrados no sistema, visualizar o histórico em formato tabular e registrar novos eventos manualmente através de formulário dedicado.

**Why this priority**: Permite visibilidade operacional e intervenção humana direta quando necessário, viabilizando acompanhamento visual das operações da fábrica.

**Independent Test**: Acessar a interface gráfica, preencher o formulário para submeter um novo evento e confirmar se o evento aparece imediatamente na tabela de listagem com feedback visual de sucesso.

**Acceptance Scenarios**:

1. **Given** que existem eventos registrados no sistema, **When** o operador acessa a seção de consulta da interface, **Then** o sistema exibe uma tabela listando os eventos com identificador, tipo, descrição e data/hora, ordenados por padrão do mais recente para o mais antigo (`timestamp DESC`) e com suporte a paginação.
2. **Given** que o operador preenche o formulário web com os dados de um novo evento e submete, **When** a solicitação é processada com sucesso, **Then** a interface apresenta uma mensagem clara de sucesso e atualiza a listagem tabular incluindo o novo evento no topo da lista.
3. **Given** que o operador tenta submeter um evento com identificador já existente através do formulário web, **When** a resposta de duplicidade é recebida, **Then** a interface exibe uma mensagem de erro compreensível informando que o identificador já se encontra cadastrado.

---

### User Story 5 - Monitoramento Operacional e Indicadores de Negócio (Priority: P3)

Gestores e equipes de suporte operacional necessitam monitorar a saúde do sistema e métricas de desempenho do processamento de eventos (total de eventos processados, eventos duplicados evitados, volumes por tipo e tempos de resposta).

**Why this priority**: Fornece transparência operacional, detecção proativa de gargalos e medição da confiabilidade do sistema ao longo do tempo.

**Independent Test**: Submeter uma série de eventos (normais, duplicados, produção e qualidade) e verificar se os contadores e tempos de execução refletem exatamente as operações realizadas.

**Acceptance Scenarios**:

1. **Given** o sistema em operação, **When** eventos são processados com sucesso ou rejeitados por duplicidade, **Then** os indicadores operacionais agregam contadores específicos para eventos processados, eventos duplicados, eventos de produção e eventos de qualidade.
2. **Given** ferramentas de monitoramento consultando a integridade do sistema, **When** o serviço está saudável e conectado às suas dependências essenciais, **Then** o sistema informa status operacional positivo e indicadores vitais de disponibilidade.

---

### Edge Cases

- **O que acontece se um evento for enviado com identificador idêntico ao de um evento existente, mas com descrição ou tipo alterados?**
  O sistema rejeita a operação com erro de conflito/duplicidade baseado estritamente na unicidade do identificador (`eventId`). A mutação arbitrária de eventos históricos é proibida.
- **O que acontece se a requisição contiver data/hora no futuro ou em formato inválido, ou omitir o campo?**
  Caso o campo `timestamp` seja omitido ou nulo, o sistema preenche automaticamente com a data/hora corrente de recebimento. Caso seja enviado com valor malformado ou em padrão incompatível com ISO 8601, a requisição é rejeitada com erro de validação.
- **Como o sistema se comporta sob rajadas de submissões simultâneas concorrentes com o mesmo identificador?**
  Apenas a primeira solicitação a obter confirmação é registrada; todas as demais são rejeitadas deterministicamente com conflito, sem corrupção ou inconsistência de dados.
- **O que acontece se o serviço de persistência estiver temporariamente indisponível?**
  O sistema retorna uma mensagem amigável de erro de servidor, sem vazar informações internas de infraestrutura, e os contadores de erro são atualizados.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST fornecer capacidade de ingestão de eventos contendo identificador único (`eventId`), tipo (`eventType`), descrição (`description`) e data/hora (`timestamp`), preenchendo o `timestamp` automaticamente com a data/hora de recebimento se este for omitido.
- **FR-002**: O sistema MUST validar a obrigatoriedade e conformidade dos campos `eventId`, `eventType` e `description` antes de qualquer etapa de processamento ou persistência, e validar o formato de `timestamp` caso explicitamente fornecido.
- **FR-003**: O sistema MUST suportar nativamente pelo menos os tipos de evento `PRODUCTION` e `QUALITY`, com arquitetura extensível para novos tipos futuros.
- **FR-004**: O sistema MUST validar a existência prévia do `eventId` e rejeitar qualquer tentativa de reprocessamento ou duplicidade, retornando mensagem descritiva de conflito.
- **FR-005**: O sistema MUST persistir de forma durável e consistente todos os eventos aceitos.
- **FR-006**: O sistema MUST gerar e persistir automaticamente um registro de auditoria imutável sempre que um evento do tipo `QUALITY` for processado com sucesso.
- **FR-007**: O sistema MUST assegurar que eventos do tipo `PRODUCTION` sejam apenas registrados e persistidos, sem criação de registros na trilha de auditoria.
- **FR-008**: O sistema MUST disponibilizar mecanismo de consulta para listagem de eventos com ordenação padrão decrescente por data/hora (`timestamp DESC`) e suporte a paginação simples.
- **FR-009**: O sistema MUST disponibilizar mecanismo de consulta individual de evento a partir do seu identificador único.
- **FR-010**: O sistema MUST disponibilizar interface gráfica web que permita:
  - Cadastro de novos eventos com campos para identificador, seleção de tipo e descrição;
  - Visualização de todos os eventos registrados em formato de tabela estruturada;
  - Exibição de mensagens visuais claras de sucesso ou erro (incluindo destaque para erro de duplicidade).
- **FR-011**: O sistema MUST contabilizar métricas operacionais discriminando eventos processados com sucesso, eventos duplicados rejeitados, total por tipo (`PRODUCTION` e `QUALITY`) e tempos médios de processamento.
- **FR-012**: O sistema MUST expor verificação de saúde e prontidão do serviço para fins de monitoramento e sustentação operacional.
- **FR-013**: O sistema MUST permitir acesso aberto e irrestrito aos endpoints operacionais de eventos (`/api/events`) e à interface web na execução local (com CORS habilitado para a origem da UI), aplicando restrições de credenciais de segurança exclusivamente aos endpoints de métricas sensíveis do Actuator.

### Key Entities *(include if feature involves data)*

- **Evento Industrial (`Event`)**: Representa uma ocorrência em equipamento fabril ou linha de produção. Contém atributos essenciais: identificador único de negócio (`eventId`), tipo do evento (`eventType`), descrição detalhada da ocorrência (`description`) e data/hora do evento (`timestamp`).
- **Registro de Auditoria (`AuditLog`)**: Registro formal e imutável de conformidade gerado exclusivamente para eventos críticos (como `QUALITY`). Contém o identificador do evento associado (`eventId`), a descrição da ação executada (ex.: "QUALITY_EVENT_REGISTERED") e a data/hora de criação do registro de auditoria.
- **Tipo de Evento (`EventType`)**: Classificação categórica da ocorrência fabril, inicialmente compreendendo `PRODUCTION` (eventos operacionais padrão) e `QUALITY` (eventos sujeitos a inspeção e auditoria).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% das tentativas de submissão de eventos com identificadores duplicados são identificadas e bloqueadas, mantendo taxa zero de duplicação na base de dados.
- **SC-002**: 100% dos eventos processados do tipo `QUALITY` geram o respectivo registro de auditoria, enquanto 0% dos eventos `PRODUCTION` geram registros desnecessários na trilha de auditoria.
- **SC-003**: 95% das requisições de ingestão de eventos recebem confirmação ou notificação de conflito em menos de 1 segundo sob condições normais de operação.
- **SC-004**: Os operadores conseguem submeter um novo evento e visualizar sua inclusão na tabela da interface web em menos de 3 segundos no fluxo completo.
- **SC-005**: 100% das operações de ingestão, duplicidade e distribuição por tipo de evento alimentam os contadores de métricas em tempo real.
- **SC-006**: A interface gráfica apresenta 100% das mensagens de retorno (sucesso, validação e conflito de duplicidade) em linguagem clara e acessível a operadores não técnicos.

## Assumptions

- O ambiente fabril ou os operadores fornecem identificadores de eventos únicos e previsíveis (ex.: prefixos por equipamento ou geradores sequenciais/UUIDs como `EVT-1001`).
- Os relógios dos equipamentos emissores estão razoavelmente sincronizados, ou utilizam carimbos de tempo em formato UTC / ISO 8601. Caso omitido, o sistema assume o horário de recepção.
- A entrega local prioriza simplicidade operacional: os endpoints de negócio não requerem autenticação por token ou login na UI, ficando mecanismos avançados como OAuth2/Cognito reservados para a evolução em nuvem.
- A interface web e os equipamentos emissores operam em rede local conectada com latência típica de rede industrial/corporativa.
- A evolução futura para computação em nuvem (AWS) respeitará os mesmos contratos de dados e entidades de negócio estabelecidos nesta especificação.
