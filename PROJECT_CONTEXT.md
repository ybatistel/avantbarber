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
| n8n (automação via WhatsApp) | Em integração (API pronta, workflow ainda não ligado) | API key no header `X-API-Key`, com allow-list restrita de endpoints (seção 8) |
| Painel administrativo | Futuro, sem stack definida | A definir |

Qualquer skill que gere/documente endpoints deve pensar nesses consumidores atuais e
futuros, mas **sem implementar nada que nenhum deles precisa hoje** (ex: não criar
endpoint de listagem de agendamentos por cliente "para o n8n" — ele guarda o
`agendamentoId` por conta própria).

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
  cpf/numero) e `/servicos-desejados/publico`. Para o n8n (máquina-a-máquina) existe uma
  segunda chain de segurança, acionada pelo header `X-API-Key`, com allow-list fechada de
  endpoints (seção 8); a chain do Google OAuth não mudou.
- `front-end/` é a landing page institucional (seção 3) — consome só os dois endpoints
  públicos acima, não agenda nada.

## 7. Domínio — entidades atuais

- **Cliente**: nome, cpf (opcional, único quando informado), número (obrigatório, único,
  só dígitos), senha (opcional, armazenada com hash BCrypt), endereço (opcional).
- **Barbeiro**: nome, número, cpf, senha, perfil (ADMIN | BARBEIRO).
- **ServicoDesejado**: nome, preço, duração em minutos (múltiplo de 30). Hoje **um único
  serviço** por agendamento.
- **Agendamento**: data/hora, status, origem (MANUAL | AUTOMACAO), vínculo com um Cliente,
  um Barbeiro e **um** ServicoDesejado.
- **StatusAgendamento** (atual): PENDENTE, CONFIRMADO, CANCELADO, CONCLUIDO, REAGENDADO.

## 8. Domínio — regras de negócio já implementadas

- Não é possível agendar no passado.
- Horário de funcionamento por dia da semana, hoje fixo no código
  (`AgendamentoService`): fechado aos sábados, domingo 9h–14h, segunda 13h30–19h, terça
  a sexta 10h–19h.
- Sem double-booking: um barbeiro ou cliente não pode ter dois agendamentos com
  intervalos `[início, início+duração)` sobrepostos (ver duração de serviço abaixo).
- Slots de disponibilidade calculados em intervalos de 30 minutos.
- **Duração variável por serviço** (`ServicoDesejado.duracaoMinutos`, em minutos, sempre
  múltiplo de 30 — validado em `ServicoDesejadoService`; serviços cadastrados antes dessa
  regra assumem default de 30min via `columnDefinition` na coluna, já que o schema é
  `ddl-auto: update` sem migrations). A disponibilidade (`listarHorariosDisponiveis`,
  que agora recebe `servicoId` além de `barbeiroId`/`data`) só oferece um horário de
  início se o serviço completo (início + duração) couber dentro do expediente do dia e
  não sobrepuser nenhum agendamento já ocupado — cada agendamento ocupado bloqueia pela
  duração do SEU PRÓPRIO serviço, não a do serviço sendo consultado. `salvar`/`reagendar`
  aplicam a mesma regra de sobreposição de intervalo, independente do que
  `listarHorariosDisponiveis` ofereceu (API é fonte única de verdade — relevante para
  quando o n8n passar a criar agendamentos direto). Mudança de contrato: o endpoint
  `GET /agendamentos/disponiveis` agora exige `servicoId` como parâmetro obrigatório.
- **Confirmação de agendamento (PENDENTE → CONFIRMADO)** — `PUT /agendamentos/{id}/confirmar`
  (`AgendamentoService.confirmar`). Só é permitido para agendamentos PENDENTES; qualquer
  outro status é rejeitado.
- **Origem do agendamento (MANUAL | AUTOMACAO)** — todo agendamento registra se foi criado
  manualmente pelo barbeiro ou via automação (n8n). A origem é **derivada da credencial,
  não do body**: `POST /agendamentos` autenticado pela API key do n8n sempre grava
  AUTOMACAO, sobrescrevendo o campo `origem` do request (mesmo omitido ou MANUAL) —
  `AgendamentoController.salvar`. Requisições do barbeiro (Google) podem informar a
  origem e, se omitida, assume MANUAL. É a base da trava de segurança do fluxo automático
  (ver seção 3): sem isso um workflow que omitisse `origem` escaparia do limite abaixo.
- **Limite de PENDENTES simultâneos por cliente via automação** — um cliente não pode ter
  mais que **3** agendamentos PENDENTES criados por automação ao mesmo tempo; acima disso,
  novas tentativas do n8n são bloqueadas até algum ser confirmado/cancelado. Vale só para
  origem AUTOMACAO, não para os criados manualmente pelo barbeiro. A validação trava o
  cliente (lock pessimista) para evitar que requisições concorrentes ultrapassem o limite.
- **CPF e senha do Cliente são opcionais** — cadastro manual e via automação (um cliente
  vindo de conversa de WhatsApp não informa CPF nem senha só para marcar um corte). CPF é
  único quando informado; senha, quando informada, é armazenada com hash BCrypt.
- **Número de WhatsApp identifica o Cliente (único)** — `Cliente.numero` é único
  (`unique = true` em `model/Cliente`) e **normalizado para só dígitos** (remove `+`,
  espaços, parênteses e traços — `ClienteService.normalizarNumero`) ao salvar, atualizar
  e buscar, para que `+55 (11) 99999-9999` e `5511999999999` sejam o mesmo cliente. Salvar
  ou atualizar com um número que já pertence a outro cliente lança `ChaveDuplicadaException`
  (409, como o CPF) — inclusive quando duas requisições simultâneas passam pela checagem
  prévia e só a constraint do banco barra a segunda (traduzida para 409, não 500). Número
  válido = **ao menos 1 dígito e no máximo 20** após a normalização; senão
  `BusinessException` (400) em salvar, atualizar e buscar (um valor como `"abc"` nunca é
  gravado como número vazio). O n8n faz *buscar-ou-criar* via `GET /clientes/busca?numero=...`,
  que devolve o cliente, 404 se não existir (aí o n8n cria) ou 400 se o número for inválido.
- **Endereço do Cliente é opcional** — mesma lógica de CPF e senha (cliente vindo de
  conversa de WhatsApp não informa endereço); vale para cadastro manual e via automação.
- **Escopo do n8n (autenticação por API key)** — o n8n se autentica enviando o header
  `X-API-Key`; a chave vem de `n8n.api-key` (env `N8N_API_KEY`; vazia = acesso do n8n
  desligado, falha fechada). Requisições com esse header caem numa `SecurityFilterChain`
  própria (`@Order(1)`, stateless, sem CSRF) cuja allow-list é exatamente o que o n8n pode
  fazer: `GET /agendamentos/disponiveis`, `GET /clientes/busca`, `POST /clientes`,
  `POST /agendamentos` (origem AUTOMACAO, nasce PENDENTE, sujeito ao limite de 3),
  `PUT /agendamentos/{id}/cancelar` e `PUT /agendamentos/{id}/reagendar`, mais os GET
  públicos de serviços e barbeiros. Todo o resto é negado com 403 — em especial o n8n **não
  confirma** agendamento (só o barbeiro) e **não lê** todos os agendamentos/clientes (há
  PII, ex: `cpfCliente`). Para cancelar ou reagendar, o n8n guarda o `agendamentoId`
  retornado na criação (no estado do workflow/conversa) — não existe, nem se planeja,
  endpoint de listagem de agendamentos por cliente para o n8n (YAGNI).

### Riscos conhecidos (não resolvidos, registrados de propósito)

- `cancelar` não tem guarda de status: hoje permite cancelar um agendamento CONCLUIDO. Fica
  mais relevante agora que o n8n pode chamá-lo.
- Cancelar/reagendar pelo n8n não checa o dono do agendamento (opera só por
  `agendamentoId`, sequencial) e `reagendar` devolve o agendamento para PENDENTE sem
  reaplicar o limite de 3 pendentes. Trade-off aceito (o n8n guarda o id; sem endpoint de
  listagem por cliente) — a reconsiderar, p. ex. exigindo `clienteId` nessas operações.
- A constraint `unique` em `Cliente.numero` com `ddl-auto: update` **falha na subida** se o
  banco já tiver números duplicados (ou com formatações diferentes que viram o mesmo número
  ao normalizar). Checar/limpar os dados existentes antes do deploy; registros antigos
  também continuam sem normalização até serem atualizados.

## 9. Domínio — regras esperadas, ainda NÃO implementadas no código

Estas fazem parte do escopo real do domínio (o usuário confirmou que são necessárias),
mas o código atual ainda não as reflete. Qualquer skill que trabalhe com domínio de
agendamento deve saber que existe essa lacuna entre "como é hoje" e "como deveria ser":

- **Múltiplos serviços por agendamento** (ex: corte + barba no mesmo agendamento) — hoje
  é 1:1 com `ServicoDesejado`.
- **Intervalos/indisponibilidades do barbeiro** (almoço, férias, folga, bloqueio manual)
  — hoje só existe o horário fixo por dia da semana, sem conceito de exceção pontual.
- **Status expandido**, incluindo "Não Compareceu" (no-show) — o enum atual não cobre
  esse caso (tem `REAGENDADO`, mas não tem algo equivalente a não-comparecimento).

## 10. Explicitamente fora de escopo agora (YAGNI)

- Política de cancelamento/reagendamento com prazos mínimos.
- Penalidade por no-show.
- Multi-tenant / entidade Barbearia.
- Notificação em tempo real ao barbeiro via API/backend — fica a cargo do workflow do
  n8n (ver seção 3), não é responsabilidade do Avant.
- Stack de front-end além da landing page institucional atual.

## 11. Decisões em aberto (não resolver preventivamente)

- Stack do futuro painel administrativo.
- Modelagem exata de intervalos/indisponibilidade do barbeiro (seção 9) — regra
  confirmada como necessária, mas desenho ainda não definido.
