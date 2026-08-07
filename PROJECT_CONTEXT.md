# PROJECT_CONTEXT.md

Fonte de verdade sobre o **negócio e o domínio** do AvantBarber, para ser lida pelas
skills do projeto antes de qualquer decisão de implementação, revisão ou documentação.
Para arquitetura técnica de código (camadas, comandos de build, convenções de código),
ver `CLAUDE.md` — este documento foca em negócio, domínio e roadmap, que não são
deriváveis diretamente do código.

## 1. Visão geral

AvantBarber começou como sistema interno de **uma barbearia real** (single-tenant, uso
real, não é projeto de estudo). É desenhado com boas práticas que **não bloqueiam** uma
eventual evolução para SaaS multi-tenant no futuro — mas essa evolução **não está em
escopo agora** e não deve gerar complexidade antecipada (YAGNI é princípio explícito do
projeto).

## 2. Estágio atual e prioridade

O projeto está na fase de **construção do core de negócio**. A prioridade é uma API
sólida, com domínio bem modelado e regras de negócio corretas — não velocidade de
entrega, não front-end, não automação ainda. Front-end e integração com n8n vêm depois
que o back-end estiver estável.

## 3. Fluxo operacional

**Hoje:**
1. Cliente combina tudo com a barbearia pelo **WhatsApp** (canal oficial de
   agendamento).
2. O **barbeiro** confirma disponibilidade manualmente e registra o agendamento no
   sistema — o cliente não interage com o sistema diretamente.
3. A **landing page** é só institucional: apresenta a barbearia e direciona o cliente
   pro WhatsApp. Ela não agenda nada, só consome dados públicos da API (serviços,
   barbeiros, horários de funcionamento).

**Futuro (planejado, ainda não implementado):**
- O **n8n** passa a consumir a API para, a partir da conversa no WhatsApp: checar
  disponibilidade, cadastrar cliente e criar agendamento automaticamente, sem o barbeiro
  no loop no momento da criação.
- Independentemente da origem da requisição (barbeiro via painel, ou n8n via API), **toda
  validação e persistência acontece na API** — ela é a fonte única de verdade do sistema.
- Travas de segurança confirmadas para agendamento criado via automação (detalhe em
  seção 9): nasce **PENDENTE** (igual a qualquer agendamento hoje), fica marcado com
  **origem = AUTOMACAO**, e um mesmo cliente não pode ter mais que **3 PENDENTES
  simultâneos** criados por automação. Notificar o barbeiro em tempo real é
  responsabilidade do **workflow do n8n** (ele já fala com o WhatsApp) — não é
  responsabilidade da API.
- Um **painel administrativo** será construído para a gestão da barbearia (stack ainda
  não definida).

## 4. Consumidores da API

| Consumidor | Status | Acesso |
|---|---|---|
| Barbeiro (uso interno, hoje manual) | Ativo | Login OAuth2 Google |
| Landing page institucional | Curto prazo | Somente dados públicos (serviços, barbeiros, horários) — sem agendamento |
| n8n (automação via WhatsApp) | Planejado, sem data | Auth ainda não definida — decisão adiada para o momento da implementação |
| Painel administrativo | Futuro, sem stack definida | A definir |

Qualquer skill que gere/documente endpoints deve pensar nesses consumidores atuais e
futuros, mas **sem implementar nada que nenhum deles precisa hoje** (ex: não criar
autenticação de API key "para o n8n" antes de a integração existir).

## 5. Princípios de design do projeto

- **API como fonte única de verdade** — toda regra de negócio e persistência vive na
  API, nunca em automações externas (n8n) ou clientes (landing page, painel).
- **YAGNI** — não implementar multi-tenant, políticas de cancelamento/no-show, ou auth
  de integração antes de haver necessidade real. Evitar acoplamentos que dificultariam
  essas evoluções, mas sem construir a estrutura antes da hora.
- **Single-tenant hoje** — não existe entidade `Barbearia`/`Tenant`. Todo dado é
  implicitamente de uma única barbearia.

## 6. Arquitetura técnica (resumo — detalhe completo em `CLAUDE.md`)

- Spring Boot (Java 25) em `avant/`, Maven, arquitetura em camadas
  (`controller → service → repository`), DTOs manuais (sem MapStruct), exceções de
  domínio centralizadas em `RestExceptionHandler`, Postgres com schema derivado das
  entidades (`ddl-auto: update`, sem migrations).
- Segurança: OAuth2 login (Google) para o barbeiro/admin. Dois endpoints GET públicos
  (sem login) existem para a landing page: `/barbeiros/publico` (só id+nome, nunca
  cpf/numero) e `/servicos-desejados/publico`. Nenhum mecanismo de autenticação
  máquina-a-máquina existe ainda (relevante para quando o n8n chegar).
- `front-end/` é a landing page institucional (seção 3) — consome só os dois endpoints
  públicos acima, não agenda nada.

## 7. Domínio — entidades atuais

- **Cliente**: nome, cpf (opcional — ver seção 9), número, senha (opcional — ver seção
  9), endereço.
- **Barbeiro**: nome, número, cpf, senha, perfil (ADMIN | BARBEIRO).
- **ServicoDesejado**: nome, preço. Hoje **um único serviço** por agendamento.
- **Agendamento**: data/hora, status, vínculo com um Cliente, um Barbeiro e **um**
  ServicoDesejado. Vai ganhar um campo de **origem** (MANUAL | AUTOMACAO) — ver seção 9.
- **StatusAgendamento** (atual): PENDENTE, CONFIRMADO, CANCELADO, CONCLUIDO, REAGENDADO.

## 8. Domínio — regras de negócio já implementadas

- Não é possível agendar no passado.
- Horário de funcionamento por dia da semana, hoje fixo no código
  (`AgendamentoService`): fechado aos sábados, domingo 9h–14h, segunda 13h30–19h, terça
  a sexta 10h–19h.
- Sem double-booking: um barbeiro ou cliente não pode ter dois agendamentos no mesmo
  timestamp exato.
- Slots de disponibilidade calculados em intervalos fixos de 30 minutos.

## 9. Domínio — regras esperadas, ainda NÃO implementadas no código

Estas fazem parte do escopo real do domínio (o usuário confirmou que são necessárias),
mas o código atual ainda não as reflete. Qualquer skill que trabalhe com domínio de
agendamento deve saber que existe essa lacuna entre "como é hoje" e "como deveria ser":

- **Múltiplos serviços por agendamento** (ex: corte + barba no mesmo agendamento) — hoje
  é 1:1 com `ServicoDesejado`.
- **Duração variável por serviço** — hoje os slots são fixos de 30min; um agendamento
  com serviços mais longos deve ocupar mais de um slot, e a disponibilidade deve
  considerar a duração total, não só o horário de início.
- **Intervalos/indisponibilidades do barbeiro** (almoço, férias, folga, bloqueio manual)
  — hoje só existe o horário fixo por dia da semana, sem conceito de exceção pontual.
- **Status expandido**, incluindo "Não Compareceu" (no-show) — o enum atual não cobre
  esse caso (tem `REAGENDADO`, mas não tem algo equivalente a não-comparecimento).
- **Confirmação de agendamento (PENDENTE → CONFIRMADO)** — bloqueante para o n8n. Hoje
  não existe, em lugar nenhum do código, uma forma de transicionar um agendamento de
  PENDENTE para CONFIRMADO (só existem cancelar e reagendar). Confirmado como lacuna a
  fechar antes da integração com o n8n fazer sentido, já que "PENDENTE até o barbeiro
  confirmar" não tem como o barbeiro de fato confirmar hoje.
- **Origem do agendamento (MANUAL | AUTOMACAO)** — todo agendamento passa a registrar se
  foi criado manualmente pelo barbeiro ou via automação (n8n). É a base da trava de
  segurança do fluxo automático (ver seção 3).
- **Limite de PENDENTES simultâneos por cliente via automação** — um cliente não pode
  ter mais que **3** agendamentos PENDENTES criados por automação ao mesmo tempo; acima
  disso, novas tentativas do n8n são bloqueadas até algum ser confirmado/cancelado. Esse
  limite vale só para agendamentos com origem AUTOMACAO, não para os criados manualmente
  pelo barbeiro.
- **CPF e senha do Cliente deixam de ser obrigatórios** — hoje são `NOT NULL` em
  `model/Cliente`, o que é incompatível com um cliente criado a partir de uma conversa de
  WhatsApp (não faz sentido pedir CPF e senha só para marcar um corte). Confirmado que
  isso vale para Cliente em geral (cadastro manual e via automação), não é uma exceção
  isolada do fluxo do n8n.

## 10. Explicitamente fora de escopo agora (YAGNI)

- Política de cancelamento/reagendamento com prazos mínimos.
- Penalidade por no-show.
- Multi-tenant / entidade Barbearia.
- Notificação em tempo real ao barbeiro via API/backend — fica a cargo do workflow do
  n8n (ver seção 3), não é responsabilidade do Avant.
- Stack de front-end além da landing page institucional atual.

## 11. Decisões em aberto (não resolver preventivamente)

- **Como o n8n vai se autenticar na API** — mecanismo técnico ainda não escolhido (API
  key, OAuth2 client credentials, etc.); o *comportamento de negócio* do que o n8n pode
  fazer já está confirmado (seção 3 e 9), só falta o mecanismo em si.
- Stack do futuro painel administrativo.
- Modelagem exata de intervalos/indisponibilidade do barbeiro (seção 9) — regra
  confirmada como necessária, mas desenho ainda não definido.
