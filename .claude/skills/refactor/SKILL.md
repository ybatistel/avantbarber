---
name: refactor
description: Reestrutura código existente no AvantBarber preservando comportamento — remove duplicação, corrige violação de SOLID/Clean Architecture, elimina código morto (ex: a classe business/RegraHorarioFuncionamento.java órfã). Não muda regra de negócio nem contrato de API. Independente do fluxo linear das outras skills; pode ser invocada a qualquer momento via /refactor.
---

# Refactor — AvantBarber

## Por que essa skill existe

`/review` pode apontar duplicação ou violação de SOLID, mas não corrige — corrigir sem
mudar comportamento é uma responsabilidade separada o suficiente pra merecer sua própria
skill, especialmente porque o risco principal de uma refatoração é justamente mudar
comportamento por acidente. `/refactor` existe para fazer essa reestruturação com o
cuidado de confirmar, antes e depois, que nada de observável mudou.

## Que problemas ela resolve

- Evita que uma "limpeza de código" acabe mudando uma regra de negócio sem querer —
  especialmente arriscado quando duas partes do código parecem fazer a mesma coisa de
  formas diferentes (ex: `AgendamentoService` calcula horário de funcionamento de um
  jeito, e a classe não-referenciada `business/RegraHorarioFuncionamento.java` modela
  isso de outro jeito — consolidar as duas exige confirmar que não são duas *intenções*
  de negócio divergentes antes de simplesmente apagar uma).
- Evita refatorar código sem rede de segurança: se a área não tem teste, o risco de
  quebrar algo silenciosamente é maior.
- Mantém a reestruturação separada da adição de funcionalidade — uma coisa de cada vez.

## Quais outras skills ela pode consultar

- **`/domain`** — sempre que a refatoração envolver consolidar ou remover algo que
  *poderia* representar uma regra de negócio divergente (não só duplicação estrutural
  óbvia). Se for claramente código morto/duplicação estrutural sem ambiguidade de
  negócio, não precisa consultar.
- **`/architect`** — se a refatoração estabelecer ou esclarecer uma convenção técnica
  que ainda não está documentada em `CLAUDE.md`, para registrá-la (`/refactor` não edita
  `CLAUDE.md` diretamente).

Consultar é uma chamada interna, dentro do mesmo turno: `/refactor` invoca a skill
consultada e aguarda a resposta antes de continuar, sem precisar que o usuário rode o
comando manualmente.

## Quais responsabilidades pertencem a outras skills

- Não decide ou muda regra de negócio → `/domain` decide; se a refatoração exigiria
  mudar comportamento, ela deixou de ser refatoração e vira trabalho de `/feature`
  (depois de passar por `/architect`/`/domain`).
- Não adiciona funcionalidade nova → `/feature`.
- Não escreve teste novo — usa os testes existentes como rede de segurança; se não
  houver cobertura suficiente na área, recomenda `/test` antes de prosseguir, mas não
  escreve o teste ela mesma.
- Não decide encaixe arquitetural de uma mudança nova → `/architect`.
- Não documenta contrato de API → `/api-docs` (e só entraria em jogo se a refatoração,
  por engano, tocasse contrato — o que não deveria acontecer, ver passo 5).

## Fontes de verdade

- `CLAUDE.md` (mantido pelo `/architect`) — padrões técnicos e convenções que a
  refatoração deve seguir/reforçar.
- `PROJECT_CONTEXT.md` (mantido pelo `/domain`) — para reconhecer se algo que parece
  duplicação estrutural na verdade reflete uma decisão de negócio (seção 7–11) antes de
  eliminar um dos lados.
- Se as duas fontes conflitarem sobre qual deveria ser o padrão correto para a área
  sendo refatorada, pare e sinalize em vez de escolher uma.

## Como proceder

### 1. Identificar o escopo

A partir de um pedido direto do usuário, ou de um achado de `/review` (duplicação,
violação de SOLID, código morto).

### 2. Confirmar que é refatoração segura, não mudança de negócio disfarçada

Se o que está sendo consolidado/removido poderia representar uma regra de negócio
diferente (não só uma forma diferente de escrever a mesma regra), consulte `/domain`
antes de prosseguir. Exemplo concreto no projeto: antes de remover ou fundir
`business/RegraHorarioFuncionamento.java` com a lógica de horário em
`AgendamentoService`, confirme qual das duas é a intenção correta — não assuma que a não
referenciada é sempre a errada só por estar órfã.

### 3. Checar cobertura de teste da área

Se a área já tem teste cobrindo o comportamento relevante, use isso como rede de
segurança (rode antes e depois). Se não tiver, avise o usuário do risco e recomende
`/test` cobrir o comportamento atual antes de refatorar — especialmente para lógica de
negócio como regras de agendamento.

### 4. Refatorar

Aplique a mudança estrutural (extrair, remover duplicação, corrigir responsabilidade
única, eliminar código morto) seguindo as convenções de `CLAUDE.md`, sem alterar:

- O comportamento observável (mesmas entradas produzem as mesmas saídas/exceções).
- Contratos de API (path, método, formato de request/response).
- Regras de negócio.

### 5. Se perceber que a mudança precisaria alterar contrato ou comportamento

Pare — isso não é mais refatoração. Explique por que, e direcione para `/architect`
(reavaliar o encaixe) em vez de continuar como se fosse uma limpeza estrutural.

### 6. Rodar os testes

Rode `./mvnw.cmd test` (ou os testes relevantes) antes e depois da mudança para
confirmar que o comportamento não mudou.

### 7. Encerrar

Resuma o que foi reestruturado e confirme que os testes existentes continuam passando.
Se a refatoração expôs a falta de cobertura de teste na área, mencione isso
explicitamente e recomende `/test`.

## Quando usar

- Quando `/review` apontar duplicação, violação de SOLID/Clean Architecture ou código
  morto.
- Quando o usuário pedir uma limpeza estrutural pontual, independente do fluxo principal.

## Quando NÃO usar

- Para adicionar funcionalidade nova (`/feature`, via `/architect`/`/domain` primeiro).
- Para mudar uma regra de negócio (`/domain` decide, depois vira trabalho de `/feature`).
- Quando a "limpeza" na verdade mudaria comportamento ou contrato de API.
