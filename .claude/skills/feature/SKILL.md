---
name: feature
description: Implementa funcionalidades novas ou alterações no backend do AvantBarber (avant/) seguindo o plano aprovado pela /architect e as regras de negócio confirmadas pela /domain. Segue os padrões técnicos de CLAUDE.md (camadas, DTO manual, exceções centralizadas, transações, convenções de código). Não decide arquitetura nem regra de negócio, não escreve testes, não revisa código existente. Invocada via /feature, normalmente depois de /architect (e /domain, quando aplicável).
---

# Feature — AvantBarber

## Por que essa skill existe

É a skill que efetivamente escreve código de produção no AvantBarber. Antes dela, o
risco de misturar "decidir se algo se encaixa" com "escrever o código" na mesma
interação era gerar implementações que pulavam a validação de arquitetura ou de regra de
negócio. `/feature` assume que essas decisões (arquitetural e de negócio) já foram
tomadas — por `/architect` e `/domain` — e seu trabalho é só a execução fiel disso,
seguindo os padrões já estabelecidos no código.

## Que problemas ela resolve

- Evita implementação sem plano: se não há um encaixe arquitetural claro ou uma regra de
  negócio confirmada por trás do pedido, `/feature` não improvisa — ela pausa e devolve
  para `/architect`/`/domain`.
- Evita inconsistência de estilo: toda implementação segue os padrões já em uso no
  projeto (camadas, DTO manual, exceções, Lombok, nomenclatura), em vez de introduzir um
  jeito novo de fazer a mesma coisa.
- Evita que uma implementação feche silenciosamente uma lacuna de domínio conhecida
  (seção 9 do `PROJECT_CONTEXT.md`) sem isso ficar registrado em lugar nenhum.

## Quais outras skills ela pode consultar

- **`/domain`** — consulta interna (mesmo turno, aguarda resposta) sempre que: (a)
  durante a implementação surgir uma decisão de negócio não coberta pelo plano, ou (b) a
  implementação fechar uma lacuna da seção 9 do `PROJECT_CONTEXT.md` e precisar registrar
  a atualização (ver passo 5).
- **`/architect`** — consulta interna sempre que a implementação introduzir uma
  convenção técnica nova que precise ser registrada em `CLAUDE.md` (ver passo 6).

Consulta é diferente de handoff. Se `/feature` for invocada diretamente, sem um plano de
`/architect` já discutido nesta conversa, e a mudança não for trivial, isso **não é uma
consulta** — é um handoff de volta: `/feature` pausa sua própria execução e recomenda
rodar `/architect` primeiro (que roda seu processo completo de classificação/alinhamento
e, ao final, entrega de volta um plano para `/feature` ser invocada de novo), em vez de
assumir o encaixe arquitetural sozinha.

## Quais responsabilidades pertencem a outras skills

- Não decide se o pedido se encaixa na arquitetura → `/architect`.
- Não decide o conteúdo de uma regra de negócio → `/domain`.
- Não escreve testes → `/test`.
- Não faz auditoria de qualidade/SOLID/aderência a convenções do que foi implementado →
  `/review` (mesmo que a própria `/feature` tenha escrito o código).
- Não documenta contratos de API → `/api-docs`.
- Não reestrutura código existente sem que haja uma funcionalidade nova sendo
  implementada → `/refactor`.

## Fontes de verdade

- `PROJECT_CONTEXT.md` — contexto de negócio, mantido pelo `/domain` (não editar
  diretamente; ver passo 5).
- `CLAUDE.md` — padrões técnicos a seguir, mantido pelo `/architect`. Se a implementação
  exigir algo que `CLAUDE.md` não cobre (convenção genuinamente nova), consulte
  `/architect` antes de decidir unilateralmente (ver passo 6).
- Se `PROJECT_CONTEXT.md` e `CLAUDE.md` parecerem se contradizer para o que está sendo
  implementado, pare e reporte o conflito — isso deveria ter sido pego por `/architect`,
  mas não prossiga assumindo um dos dois se aparecer aqui.

## Como proceder

### 1. Confirmar que há plano

- Se esta conversa já tem uma análise de `/architect` (e, quando aplicável, uma resposta
  de `/domain`), use isso como especificação.
- Se `/feature` foi chamada direto, sem esse plano: avalie se a mudança é trivial e sem
  ambiguidade técnica ou de negócio. Se for, prossiga. Se não for (toca em regra de
  negócio, ou não é óbvio como se encaixa nas camadas existentes), **não decida
  sozinha** — recomende rodar `/architect` primeiro.

### 2. Implementar seguindo os padrões existentes

Siga a arquitetura em camadas do projeto (`model → repository → dto → service →
controller`, mais `exception/`+`infra/RestExceptionHandler` quando necessário):

- Controllers não acessam repository diretamente; services não devolvem `@Entity`.
- Mapeamento DTO↔entity manual (métodos privados `toDTO`/`toEntity` no service, sem
  MapStruct/ModelMapper), com DTO de request e de response separados quando fizer
  sentido.
- Erros de negócio lançam uma exceção de `exception/`; se for um tipo novo de erro,
  registre o handler correspondente em `RestExceptionHandler`.
- Métodos de escrita em `service/` que alteram estado são `@Transactional`; leitura não.
- Lombok nas entidades (`@Getter/@Setter/@NoArgsConstructor/@AllArgsConstructor/@Builder`),
  `@RequiredArgsConstructor` em service/controller, nomenclatura de métodos e variáveis
  em português, consistente com o resto do código.
- Sem scripts de migration — o schema vem de `ddl-auto: update`.

### 3. Se surgir uma decisão de negócio não coberta

Se no meio da implementação aparecer um caso que o plano de `/architect`/`/domain` não
previu (ex: uma regra de borda), pare esse trecho, consulte `/domain`, e só continue
depois de ter a regra confirmada.

### 4. Se surgir um conflito técnico não previsto

Se durante a implementação você perceber que dois padrões existentes no código se
contradizem para este caso, ou que `CLAUDE.md`/`PROJECT_CONTEXT.md` conflitam, pare e
reporte — não resolva silenciosamente escolhendo um dos dois.

### 5. Se a implementação fechar uma lacuna de domínio conhecida

Se o que foi implementado corresponde a algo listado como lacuna na seção 9 do
`PROJECT_CONTEXT.md` (ex: múltiplos serviços por agendamento, duração variável, status
"Não Compareceu"), **consulte `/domain`** (chamada interna, mesma execução) para que ela
registre a lacuna como fechada, movendo o item da seção 9 para a seção 8. `/feature` não
edita `PROJECT_CONTEXT.md` diretamente — isso é sempre feito por `/domain`, mesmo quando
quem identifica a necessidade da atualização é `/feature`. Ao final, confirme ao usuário
que a implementação foi feita e que `PROJECT_CONTEXT.md` foi atualizado.

### 6. Se a implementação introduzir uma convenção técnica nova

Se não havia um padrão existente pra esse caso e você precisou decidir um (ex: uma nova
forma de estruturar um pacote), **consulte `/architect`** (chamada interna, mesma
execução) para avaliar e registrar a convenção em `CLAUDE.md` — `/feature` não edita
`CLAUDE.md` diretamente, essa propriedade é do `/architect`.

### 7. Encerrar com o próximo passo do fluxo

Ao final, resuma o que foi implementado e lembre que o próximo passo recomendado do
fluxo é `/test` (cobrir com testes) e, depois, `/review`.

## Quando usar

- Depois que `/architect` aprovou o encaixe técnico (e `/domain` confirmou a regra de
  negócio, se havia impacto).
- Para implementar diretamente uma mudança claramente técnica, trivial e sem ambiguidade.

## Quando NÃO usar

- Para decidir se um pedido se encaixa na arquitetura (`/architect`).
- Para decidir ou esclarecer uma regra de negócio (`/domain`).
- Para escrever testes (`/test`).
- Para revisar/auditar código já existente (`/review`).
- Para reestruturar código sem adicionar funcionalidade (`/refactor`).
