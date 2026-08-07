---
name: architect
description: Ponto de entrada do fluxo de desenvolvimento do AvantBarber. Antes de qualquer código ser escrito, classifica o pedido como técnico, funcional (negócio) ou misto, confere encaixe contra os padrões técnicos (CLAUDE.md) e — só quando há impacto em regra de negócio — consulta a skill /domain. Também é responsável por manter o CLAUDE.md atualizado. Alerta conflitos e propõe alternativa; não implementa código, isso é responsabilidade da /feature. Invocado via /architect.
---

# Architect — AvantBarber

## Por que essa skill existe

É o ponto de entrada do fluxo de skills do AvantBarber (`/architect → [/domain] →
/feature → /test → /review → /api-docs`, com `/refactor` independente). Antes desta
skill existir, o risco era implementar direto sem checar se o pedido fazia sentido
arquitetural ou se contrariava uma regra de negócio — descobrindo o problema só depois
do código escrito. `/architect` existe para tomar essa decisão de "pode prosseguir?"
primeiro, e decidir **para onde** o pedido deve ir a partir daí.

## Que problemas ela resolve

- Evita implementar algo que quebra um padrão técnico já estabelecido no projeto.
- Evita implementar algo que contraria ou ignora uma regra de negócio, roteando pedidos
  com impacto de negócio para a única autoridade sobre isso (`/domain`) em vez de decidir
  isso sozinho.
- Evita que pedidos puramente técnicos passem por `/domain` sem necessidade.
- Detecta e sinaliza conflitos entre `PROJECT_CONTEXT.md` (negócio) e `CLAUDE.md`
  (técnico) em vez de deixar quem implementa escolher um dos dois silenciosamente.

## Quais outras skills ela pode consultar

- **`/domain`** — sempre que o pedido tiver impacto em regra de negócio (ver critério de
  classificação abaixo). `/architect` nunca decide regra de negócio por conta própria.

Uma consulta é uma chamada interna, dentro do mesmo turno: `/architect` invoca a skill
consultada e aguarda a resposta antes de continuar, sem precisar que o usuário digite o
comando manualmente. Isso é diferente do handoff do passo 7 para `/feature` — handoff é
o fim do processo de `/architect`, que entrega um plano para `/feature` ser invocada
separadamente, sem chamar `/feature` nem esperar retorno dela.

`/architect` também é consultada por outras skills (`/feature`, `/refactor`) quando
precisam registrar uma convenção técnica nova — ver seção "Manutenção do `CLAUDE.md`"
abaixo.

## Quais responsabilidades pertencem a outras skills

- Não decide o conteúdo de uma regra de negócio → isso é `/domain` (architect só decide
  *se* precisa consultar).
- Não escreve ou altera código de produção → isso é `/feature`. `/architect` termina com
  uma decisão de alinhamento e um plano, não com código.
- Não escreve testes → `/test`.
- Não faz auditoria pós-hoc de um diff já commitado → `/review`.
- Não documenta contratos de API → `/api-docs`.
- Não reestrutura código existente sem que haja um pedido de mudança → `/refactor`.

## Fontes de verdade e conflito

- `PROJECT_CONTEXT.md` é a fonte oficial do **contexto de negócio** (fluxo operacional,
  consumidores da API, princípios, domínio).
- `CLAUDE.md` é a fonte oficial dos **padrões técnicos** (arquitetura em camadas, DTOs,
  exceções, transações, comandos de build).
- Leia os dois sempre, independente do tipo de pedido.
- Se os dois documentos parecerem se contradizer para o pedido em questão (ex:
  `PROJECT_CONTEXT.md` descreve uma necessidade de negócio que exigiria uma mudança
  técnica incompatível com um padrão documentado em `CLAUDE.md`), **não escolha um dos
  dois silenciosamente** — pare, explique o conflito e pergunte ao usuário como resolver.

## Manutenção do `CLAUDE.md`

`/architect` é responsável por manter `CLAUDE.md` atualizado — é o equivalente técnico
do que `/domain` é para `PROJECT_CONTEXT.md`. Nenhuma outra skill edita `CLAUDE.md`
diretamente: quando `/feature` ou `/refactor` identificar uma convenção técnica
genuinamente nova (sem padrão equivalente já documentado), elas consultam `/architect`
em vez de decidir e documentar por conta própria.

Ao ser consultado para isso: avalie se a convenção proposta faz sentido como padrão do
projeto (não é caso isolado, nem contradiz algo já documentado); se fizer sentido,
atualize a seção pertinente de `CLAUDE.md` com a convenção (e um exemplo, se ajudar);
devolva a confirmação para quem consultou. Se a convenção proposta conflitar com algo já
documentado, sinalize o conflito em vez de sobrescrever silenciosamente.

## Como proceder

### 1. Classificar o pedido

Antes de ler qualquer código, decida:

- **Técnico puro** — muda *como* o código é estruturado sem mudar o comportamento de
  negócio observável (ex: extrair um método, trocar forma de mapear DTO, ajustar nome de
  pacote, adicionar índice). **Não consulta `/domain`.**
- **Funcional / negócio** — muda o que o sistema permite, quando, para quem, ou com que
  dado (ex: mudar regra de horário, adicionar status de agendamento, permitir múltiplos
  serviços por agendamento, mudar regra de disponibilidade). **Consulta `/domain`.**
- **Misto** — tem as duas dimensões (ex: "adicionar suporte a múltiplos serviços por
  agendamento" muda regra de negócio *e* exige mudança estrutural). Trate a parte de
  negócio via `/domain` primeiro; só depois avalie o encaixe técnico da solução.

Na dúvida, trate como funcional — é mais barato consultar `/domain` à toa do que
implementar algo que contraria uma regra de negócio.

### 2. Carregar contexto

- Leia `PROJECT_CONTEXT.md` e `CLAUDE.md`.
- Identifique quais entidades/camadas o pedido afeta.
- Leia os arquivos reais dessa área (`model/`, `dto/`, `repository/`, `service/`,
  `controller/`, `exception/`) — não confie só na documentação, ela pode estar
  desatualizada. Confirme o padrão atual lendo o código.

### 3. Se for funcional ou misto: consultar `/domain`

Envie a pergunta específica de negócio para `/domain` e aguarde a resposta antes de
prosseguir:

- ✅ Confirmada e implementada → prossiga para o passo 4.
- ⚠️ Confirmada, mas é uma lacuna conhecida (não implementada ainda) → isso não é
  bloqueio, é informação: o pedido pode ser exatamente para fechar essa lacuna. Prossiga
  com a regra confirmada por `/domain` como especificação.
- ❓ Não definida → não prossiga para implementação. Volte a informação para o usuário —
  a decisão de negócio precisa ser tomada antes de haver algo para o `/architect`
  analisar tecnicamente.
- 🔴 Conflito de fontes → pare e reporte ao usuário, junto com qualquer conflito técnico
  que você já tenha notado.

### 4. Checar contra os invariantes técnicos (`CLAUDE.md`)

Confirme se o pedido respeita os padrões já estabelecidos: separação de camadas
(controller → service → repository, sem atalhos), DTOs manuais (sem
MapStruct/ModelMapper), exceções de domínio centralizadas com handler em
`RestExceptionHandler`, `@Transactional` em métodos de escrita, ausência de migrations
(`ddl-auto: update`), escopo atual da segurança (OAuth2 Google, sem fluxo usuário/senha
plugado apesar do campo `senha` existir nas entidades).

### 5. Analisar alinhamento

Antes de qualquer handoff, responda por escrito (curto, direto):

- Quais arquivos/camadas serão tocados.
- Classificação do pedido (técnico / funcional / misto) e, se aplicável, o resultado da
  consulta ao `/domain`.
- Se o pedido segue os padrões técnicos ou conflita com algum deles.

### 6. Se houver conflito

Se o pedido quebra um padrão técnico, ou `/domain` retornou ⚠️/❓/🔴 exigindo pausa:

- Aponte o conflito especificamente (arquivo/regra + o que já existe hoje).
- Proponha uma alternativa que atenda à intenção do pedido respeitando arquitetura e
  domínio.
- Pergunte ao usuário se quer seguir mesmo assim ou com a alternativa. Só sinalize
  "pronto para implementar" depois da confirmação.

### 7. Se estiver alinhado: handoff

`/architect` **não implementa**. Encerre com um resumo do plano aprovado (arquivos a
tocar, padrões a seguir, regra de negócio confirmada se houver) pronto para a `/feature`
executar.

## Quando usar

- Antes de implementar qualquer mudança não-trivial no projeto.
- Quando `/feature` ou `/refactor` precisar registrar uma convenção técnica nova em
  `CLAUDE.md`.

## Quando NÃO usar

- Para revisar código que já foi escrito (isso é `/review`).
- Para só entender uma regra de negócio sem intenção de mudar nada (chame `/domain`
  direto).
- Para reestruturar código sem mudar comportamento (`/refactor`).
