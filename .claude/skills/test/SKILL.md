---
name: test
description: Cria e mantém testes unitários e de integração para o backend do AvantBarber (avant/), cobrindo regras de negócio (service layer) e contratos de API (controllers). Estabelece convenção de teste no projeto — hoje só existe o teste de contexto. Não decide regra de negócio, não corrige código de produção, não faz revisão geral de qualidade. Invocada via /test, normalmente depois de /feature.
---

# Test — AvantBarber

## Por que essa skill existe

Hoje o projeto tem um único teste (`AvantApplicationTests#contextLoads`) — não existe
convenção estabelecida de como testar regra de negócio ou contrato de API no AvantBarber.
Sem isso, cada teste novo corre o risco de reinventar a abordagem (ou de subir contexto
Spring completo — com banco e OAuth2 — para casos que um teste unitário simples já
resolveria). `/test` existe para cobrir o que `/feature` implementa com o tipo de teste
certo, de forma consistente.

## Que problemas ela resolve

- Garante que regras de negócio críticas (horário de funcionamento, disponibilidade,
  double-booking, e as que forem fechadas a partir da seção 9 do `PROJECT_CONTEXT.md`)
  tenham teste automatizado, não só validação manual.
- Evita testes de integração completos (`@SpringBootTest`) para casos que não precisam
  disso — o projeto exige `DB_PASSWORD`, `GOOGLE_CLIENT_ID` e `GOOGLE_CLIENT_SECRET` sem
  default (`application.yaml`) e um Postgres acessível para subir o contexto completo;
  usar isso por padrão torna os testes lentos e frágeis desnecessariamente.
- Evita assumir bibliotecas de teste (Mockito, AssertJ) sem confirmar que estão
  disponíveis — o projeto usa os starters de teste modulares do Spring Boot 4
  (`spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, etc.) em vez
  do clássico `spring-boot-starter-test`, e o conjunto de dependências transitivas pode
  não ser o que se assumiria por hábito.

## Quais outras skills ela pode consultar

- **`/domain`** — quando, ao escrever o teste, aparecer um caso de borda cujo
  comportamento esperado não está claro ou não foi coberto pela análise original.

Consultar é uma chamada interna, dentro do mesmo turno: `/test` invoca `/domain` e
aguarda a resposta antes de continuar, sem precisar que o usuário rode `/domain`
manualmente.

## Quais responsabilidades pertencem a outras skills

- Não decide o conteúdo de uma regra de negócio → `/domain`.
- Não corrige código de produção para fazer um teste passar → se o teste revela um bug,
  reporta o achado e recomenda `/feature` para corrigir; `/test` não edita código de
  produção.
- Não faz auditoria geral de qualidade/SOLID/convenções → `/review`.
- Não documenta contratos de API → `/api-docs` (ainda que os testes de controller sirvam
  de referência para isso).

## Fontes de verdade

- `PROJECT_CONTEXT.md` (mantido pelo `/domain`) — para saber qual é a regra de negócio
  esperada por trás do que está sendo testado (inclusive lacunas da seção 9, se o teste
  for cobrir algo que acabou de ser implementado para fechar uma delas).
- `CLAUDE.md` (mantido pelo `/architect`) — comandos de build/teste (`./mvnw.cmd test`,
  como rodar uma classe específica) e estrutura de pacotes.
- Se os dois conflitarem sobre o comportamento esperado de algo que está sendo testado,
  pare e reporte — não escolha um dos dois para decidir o que o teste deve assertar.

## Como proceder

### 1. Confirmar o que precisa de teste

- O que `/feature` acabou de implementar nesta conversa, ou
- Uma regra já confirmada por `/domain` que ainda não tem cobertura.

Não escreva teste para uma regra que não está confirmada — se não estiver claro o
comportamento esperado, consulte `/domain` primeiro.

### 2. Escolher o tipo de teste certo

- **Regra de negócio isolada** (ex: validação de horário, disponibilidade, double-
  booking em `AgendamentoService`) → teste unitário, mockando os repositories, **sem**
  subir contexto Spring.
- **Contrato de endpoint** (status HTTP, validação de `@RequestBody`, formato de
  resposta, mapeamento de exceção pelo `RestExceptionHandler`) → teste de camada web
  isolado (slice test do controller), mockando o service — evita depender de banco ou
  OAuth2.
- **Integração real entre camadas/banco** → só quando o objetivo específico for validar
  isso; nesse caso, o teste precisa de `DB_PASSWORD`/`GOOGLE_CLIENT_ID`/
  `GOOGLE_CLIENT_SECRET` configurados e Postgres acessível (como no `pipeline.yaml` de
  CI). Use com moderação, não como padrão.

### 3. Confirmar dependências de teste disponíveis

Antes de usar Mockito, AssertJ ou qualquer biblioteca de asserção/mock, confira em
`avant/pom.xml` se ela está disponível (direta ou transitivamente pelos starters de
teste já presentes) em vez de assumir. Se faltar algo essencial, informe o usuário antes
de adicionar uma dependência nova.

### 4. Escrever o teste seguindo convenção do projeto

- Classe de teste espelhando o pacote da classe testada, sufixo `Test`.
- Nomes de método descritivos em português do cenário testado (consistente com o padrão
  já usado no projeto).
- Cobrir o caminho feliz **e** os casos de exceção que a regra deveria disparar
  (`BusinessException`, `HorarioFuncionamentoException`, `RecursoNaoEncontradoException`,
  `ChaveDuplicadaException`, conforme o caso).

### 5. Se o teste revelar divergência

- Se o comportamento implementado não bate com o que `/domain` confirmou como regra
  esperada: não corrija a produção você mesma. Reporte o achado e recomende voltar para
  `/feature`.
- Se aparecer um caso de borda sem regra definida: pause e consulte `/domain` antes de
  decidir o que o teste deve assertar.

### 6. Rodar os testes

Rode `./mvnw.cmd test` (ou o teste específico) para confirmar que passam antes de
reportar como concluído.

### 7. Encerrar com o próximo passo do fluxo

Ao final, indique que o próximo passo recomendado é `/review`.

## Quando usar

- Depois que `/feature` implementa algo novo ou altera comportamento existente.
- Para cobrir uma regra já confirmada por `/domain` que ainda não tem teste.

## Quando NÃO usar

- Para decidir ou esclarecer uma regra de negócio (`/domain`).
- Para corrigir um bug encontrado (volta para `/feature`).
- Para avaliar qualidade estrutural do código além de comportamento (`/review`).
- Para documentar a API (`/api-docs`).
