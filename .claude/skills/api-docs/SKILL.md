---
name: api-docs
description: Mantém a documentação e os contratos da API do AvantBarber sincronizados com o código — hoje isso significa a coleção Postman em postman/collections/Avant Barber API/. Cobre todos os consumidores da API (landing page, n8n, futuro painel administrativo, futuras integrações). Não decide design de endpoint, não implementa código, não decide regra de negócio. Invocada via /api-docs, normalmente como última etapa do fluxo depois de /review.
---

# API Docs — AvantBarber

## Por que essa skill existe

O único contrato de API documentado hoje no projeto é a coleção Postman
(`postman/collections/Avant Barber API/`) — não existe OpenAPI/Swagger nem um README de
endpoints. Essa coleção já está desatualizada em relação ao código real: a pasta
`Agendamentos/` só tem a requisição "Listar Agendamentos", enquanto
`AgendamentoController` expõe seis endpoints (`buscarTodos`, `buscarPorId`,
`listarHorariosDisponiveis`, `salvar`, `cancelar`, `reagendar`). Sem uma skill dedicada a
manter isso sincronizado, a documentação tende a ficar cada vez mais atrás do código
conforme `/feature` implementa coisas novas — e os consumidores reais (landing page, e
futuramente n8n e painel admin) dependem desse contrato estar certo.

## Que problemas ela resolve

- Evita divergência entre o que a coleção Postman documenta e o que a API realmente faz.
- Dá um contrato confiável para todos os consumidores da API listados no
  `PROJECT_CONTEXT.md` (seção 4): landing page, n8n, futuro painel administrativo,
  futuras integrações — não só o consumidor mais recente que motivou a mudança.
- Detecta quando um endpoint não atende ao que um consumidor documentado precisaria dele
  — por exemplo, se a landing page precisa consumir dados públicos sem autenticação, mas
  o endpoint em questão exige login OAuth2 porque `SpringConfig` só libera `/` e
  `/login`, isso é uma inconsistência que vale sinalizar, não documentar como se fosse
  normal.

## Quais outras skills ela pode consultar

- **`/domain`** — quando o significado de negócio de um campo/endpoint não estiver claro
  o suficiente para escrever uma descrição precisa (consulta leve, não é decisão de
  design).

Consultar é uma chamada interna, dentro do mesmo turno: `/api-docs` invoca `/domain` e
aguarda a resposta antes de continuar, sem precisar que o usuário rode `/domain`
manualmente.

## Quais responsabilidades pertencem a outras skills

- Não decide o design ou o comportamento de um endpoint → `/architect` (encaixe) e
  `/feature` (implementação) já decidiram isso antes de chegar aqui.
- Não decide regra de negócio → `/domain`.
- Não implementa ou corrige código de produção → `/feature`. Se a documentação revelar
  que um endpoint não faz o que deveria (bug) ou que existe a inconsistência de acesso
  descrita acima, `/api-docs` reporta isso e recomenda `/architect`/`/feature`, não
  corrige.
- Não escreve testes → `/test`.
- Não faz auditoria de qualidade/SOLID → `/review`.

## Fontes de verdade

- O **código real** (`controller/`, `dto/`) é a fonte de verdade do que a API faz — a
  documentação segue o código, nunca o contrário.
- `PROJECT_CONTEXT.md` (mantido pelo `/domain`, seção 4) — para saber quem consome a API
  hoje e no futuro, e o que cada consumidor precisa (ex: landing page só dados públicos,
  sem agendamento).
- `CLAUDE.md` (mantido pelo `/architect`) — para nomenclatura/estrutura de pacotes ao
  identificar o que mudou.
- Se o código real e a intenção documentada em `PROJECT_CONTEXT.md` conflitarem (ex: um
  endpoint que deveria ser público exige autenticação), não resolva isso documentando um
  dos dois como se fosse a verdade — sinalize o conflito.

## Como proceder

### 1. Determinar o escopo

- O que `/feature` acabou de implementar/alterar nesta conversa, ou
- Uma auditoria completa pedida explicitamente pelo usuário (comparar todos os
  controllers com toda a coleção Postman).

### 2. Ler o código real

Para cada endpoint no escopo, leia o controller e os DTOs envolvidos: método HTTP, path,
parâmetros, corpo de request, formato de resposta, e quais exceções o
`RestExceptionHandler` pode traduzir em quais status HTTP para esse endpoint.

### 3. Comparar com a coleção Postman

A estrutura é uma pasta por entidade em
`postman/collections/Avant Barber API/<Entidade>/`, com um arquivo
`<Nome da Requisição>.request.yaml` por endpoint (formato `$kind: http-request`, com
`name`, `method`, `url` usando `{{baseUrl}}`, `order`, `description` opcional, `headers`,
e `body` com exemplo JSON quando aplicável — use os arquivos existentes como referência
de estilo, ex: `Barbeiros/Criar Barbeiro.request.yaml`).

- Endpoint no código sem arquivo correspondente → criar.
- Arquivo cujo endpoint não existe mais no código → sinalizar para remoção (confirme com
  o usuário antes de apagar).
- Arquivo existente com method/url/body divergente do código → atualizar.

### 4. Escrever exemplos realistas

Body de exemplo deve refletir o formato real do DTO de request, com valores plausíveis
(seguindo o estilo dos exemplos já existentes na coleção). Não copiar campos que não
existem mais no DTO nem inventar campos que não existem.

### 5. Considerar os consumidores

Ao escrever ou atualizar a `description` de um endpoint, indique quando fizer sentido
qual consumidor documentado ele atende (ex: "dados públicos para a landing page", "usado
pelo barbeiro para registrar agendamento"). Isso ajuda a notar rapidamente, no futuro,
se um endpoint some ou muda de forma que quebra um consumidor específico.

### 6. Se encontrar inconsistência

Se um endpoint não suporta o que um consumidor documentado precisaria dele (ex: exigiria
autenticação que a landing page não tem, ou não expõe um dado que o n8n vai precisar
quando a integração existir), não documente isso como se fosse esperado — reporte a
inconsistência ao usuário e recomende `/architect` para decidir o que fazer.

### 7. Encerrar

Resuma o que foi criado/atualizado/removido na coleção. Esta é normalmente a última
etapa do fluxo linear (`/architect → [/domain] → /feature → /test → /review →
/api-docs`); `/refactor` continua disponível a qualquer momento, fora desse fluxo.

## Quando usar

- Depois que um endpoint é criado ou alterado e já passou por `/review`.
- Para auditar a coleção Postman inteira contra o código real, sob pedido.

## Quando NÃO usar

- Para decidir o que um endpoint deve fazer (`/architect`/`/feature`).
- Para decidir regra de negócio (`/domain`).
- Para corrigir um bug encontrado (`/feature`).
