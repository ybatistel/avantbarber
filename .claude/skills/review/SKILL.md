---
name: review
description: Audita código já escrito no AvantBarber (diff atual, branch ou PR) contra regras de negócio confirmadas (via /domain), aderência ao PROJECT_CONTEXT.md (fluxo, consumidores, princípios como YAGNI) e aos padrões técnicos de CLAUDE.md (camadas, SOLID, Clean Architecture, convenções). Reporta achados, não corrige código. Invocada via /review, normalmente depois de /test.
---

# Review — AvantBarber

## Por que essa skill existe

`/architect` valida o plano **antes** de codar; `/review` audita o **resultado** — código
que já foi escrito, seja pela `/feature`, pelo usuário, ou legado. São momentos
diferentes do fluxo e por isso skills diferentes: um plano aprovado por `/architect` não
garante que a implementação final saiu como planejado, e código legado nunca passou por
`/architect` nenhuma vez. `/review` existe para pegar esse gap antes de um commit/PR ir
adiante.

## Que problemas ela resolve

- Detecta implementação que diverge da regra de negócio confirmada, mesmo que o plano
  original estivesse certo.
- Detecta violação de princípios do projeto documentados no `PROJECT_CONTEXT.md` que não
  são "regra de negócio" no sentido estrito, mas ainda assim são contrato do projeto —
  por exemplo, complexidade adicionada antecipadamente (violação de YAGNI, como
  estruturas de multi-tenant, ou autenticação de integração antes de a integração
  existir) ou algo fora do escopo declarado na seção 10.
- Detecta violação de convenção técnica (`CLAUDE.md`) e problema estrutural (SOLID,
  Clean Architecture, duplicação) sem misturar isso com decisão de negócio.
- Dá um relatório acionável, sem tentar consertar tudo na mesma passada — cada achado
  aponta para a skill certa para resolver.

## Quais outras skills ela pode consultar

- **`/domain`** — sempre que precisar confirmar se um comportamento implementado
  corresponde à regra de negócio esperada, especialmente para código que nunca foi
  validado por `/domain` antes (ex: legado, ou mudança feita fora do fluxo).

Consultar é uma chamada interna, dentro do mesmo turno: `/review` invoca `/domain` e
aguarda a resposta antes de continuar, sem precisar que o usuário rode `/domain`
manualmente.

## Quais responsabilidades pertencem a outras skills

- Não decide o conteúdo de uma regra de negócio → `/domain` (review consulta, não
  decide).
- Não corrige o código encontrado com problema → `/feature` (bug de comportamento) ou
  `/refactor` (problema estrutural sem mudar comportamento). `/review` só reporta.
- Não escreve ou completa testes faltantes → `/test` (review pode apontar que falta
  cobertura, não escrever o teste).
- Não valida o plano antes da implementação → `/architect` (isso já deveria ter
  acontecido antes de `/review` entrar).
- Não documenta contratos de API → `/api-docs`.

## Fontes de verdade

- `PROJECT_CONTEXT.md` (mantido pelo `/domain`) — não só para regra de negócio pontual,
  mas para aderência ampla: fluxo operacional (seção 3), consumidores da API (seção 4),
  princípios do projeto (seção 5, especialmente YAGNI e "API como fonte única de
  verdade"), e o que está explicitamente fora de escopo (seção 10). Uma mudança pode
  estar tecnicamente correta e ainda assim violar isso (ex: implementar algo pensando em
  multi-tenant que o projeto explicitamente não quer agora).
- `CLAUDE.md` (mantido pelo `/architect`) — padrões técnicos e convenções de código.
- Se as duas fontes conflitarem sobre o que está sendo revisado, reporte isso como um
  achado à parte em vez de julgar o código contra uma das duas silenciosamente.

## Como proceder

### 1. Determinar o escopo

- Use o que o usuário indicou (arquivos, branch, PR) ou, na ausência disso, o diff atual
  (`git status` / `git diff`, ou commits à frente da branch principal).
- Se o escopo estiver ambíguo, pergunte antes de revisar o repositório inteiro.

### 2. Carregar contexto

Leia `PROJECT_CONTEXT.md` e `CLAUDE.md`. Identifique quais entidades/regras de negócio o
diff toca.

### 3. Checar aderência à regra de negócio

Para cada trecho do diff que implementa ou altera comportamento de negócio, confirme com
`/domain` se bate com a regra esperada — não assuma que a implementação está certa só
porque parece razoável, e não assuma que está errada sem confirmar.

### 4. Checar aderência ampla ao `PROJECT_CONTEXT.md`

Além da regra pontual, avalie: o diff respeita o fluxo operacional atual (seção 3)? Faz
sentido para os consumidores documentados (seção 4)? Introduz algo listado como fora de
escopo (seção 10) sem necessidade real? Viola YAGNI ou o princípio de API como fonte
única de verdade (seção 5)?

### 5. Checar aderência técnica ao `CLAUDE.md`

Camadas respeitadas, DTO manual, exceções centralizadas com handler registrado,
`@Transactional` correto, convenções de nomenclatura/Lombok.

### 6. Avaliar qualidade estrutural

SOLID, Clean Architecture, duplicação, acoplamento desnecessário. Aponte a oportunidade —
não implemente a correção (isso é `/refactor` se for estrutural, `/feature` se for
correção de comportamento).

### 7. Reportar achados

Use a ferramenta `ReportFindings` para listar os achados, do mais severo ao menos
severo. Para cada achado, deixe claro no resumo qual é a skill recomendada para resolvê-
lo (`/feature`, `/refactor`, `/domain`, `/test` ou `/api-docs`). Se nada relevante for
encontrado, reporte lista vazia — não invente achado para preencher o relatório.

### 8. Encerrar com o próximo passo do fluxo

Ao final, indique que o próximo passo recomendado é `/api-docs`.

## Quando usar

- Depois de `/test`, antes de commit/PR seguir adiante.
- Para auditar código legado que nunca passou pelo fluxo `/architect → /domain → /feature`.

## Quando NÃO usar

- Antes de implementar algo (isso é `/architect`).
- Para decidir regra de negócio do zero (`/domain`).
- Para corrigir o que foi encontrado (volta para `/feature` ou `/refactor`).
- Para escrever teste faltante (`/test`).
