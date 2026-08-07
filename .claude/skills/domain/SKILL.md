---
name: domain
description: Fonte única de verdade das regras de negócio do AvantBarber (clientes, barbeiros, serviços, agenda, disponibilidade, duração de serviço, múltiplos serviços por agendamento, status de agendamento). Responde como o domínio deve se comportar e sinaliza lacunas entre regra esperada e código atual. Não gera código, não revisa código, não decide arquitetura. Invocada via /domain, tipicamente chamada pelo /architect quando uma solicitação impacta regra de negócio.
---

# Domain — AvantBarber

## Por que essa skill existe

Regras de negócio do AvantBarber (horário de funcionamento, disponibilidade, duração de
serviço, múltiplos serviços por agendamento, status do agendamento, etc.) apareciam
espalhadas em conversas e, potencialmente, duplicadas dentro de outras skills
(`/architect`, `/feature`, `/review`). Isso cria risco de duas skills decidirem a mesma
regra de formas diferentes. O `/domain` existe para ser o único lugar que responde "como
o domínio deve se comportar" — todo o resto consulta, nenhuma outra skill decide regra
de negócio por conta própria.

## Que problemas ela resolve

- Evita que `/architect`, `/feature`, `/review`, `/test` e `/refactor` interpretem uma
  regra de negócio de formas diferentes entre si.
- Deixa explícita a diferença entre **regra já implementada no código** e **regra
  esperada pelo domínio mas ainda não implementada** (ver `PROJECT_CONTEXT.md`, seção 9)
  — evitar que alguém assuma que o código atual é o comportamento correto quando na
  verdade é uma lacuna conhecida.
- Dá um lugar único para registrar uma decisão de negócio nova, em vez de ela ficar só
  numa conversa e se perder.

## Quais outras skills ela pode consultar

Nenhuma. `/domain` é uma folha na árvore de dependências — ela é a autoridade, não
consulta outras skills para decidir. Ela lê `PROJECT_CONTEXT.md` (fonte oficial do
contexto de negócio) e, quando necessário, o código atual (`model/`, `service/`) para
comparar o que está implementado com o que é esperado.

## Como é consultada por outra skill

Uma consulta a `/domain` é uma chamada interna, dentro do mesmo turno de quem está
consultando: a skill consultante invoca `/domain` e aguarda a resposta antes de
continuar — não é necessário o usuário digitar `/domain` manualmente. Se a resposta for
❓ (não definida), `/domain` interage diretamente com o usuário para obter a decisão
antes de devolver a resposta a quem consultou. Ao final de qualquer consulta, o controle
volta para a skill que chamou.

## Quais responsabilidades pertencem a outras skills

`/domain` **não faz** nenhuma das coisas abaixo — se a solicitação pedir isso, redirecione
para a skill certa em vez de tentar resolver aqui:

- Não decide se algo se encaixa na arquitetura técnica → isso é `/architect`.
- Não escreve ou altera código de produção → isso é `/feature`.
- Não avalia qualidade estrutural, SOLID ou convenções de código → isso é `/review`.
- Não escreve testes → isso é `/test`.
- Não decide contratos/documentação de API → isso é `/api-docs`.
- Não reestrutura código → isso é `/refactor`.

## Fontes de verdade e conflito

- `PROJECT_CONTEXT.md` é a fonte oficial do contexto de negócio — seções 3 (fluxo
  operacional), 4 (consumidores), 5 (princípios), 7–11 (domínio) são a base de toda
  resposta.
- `CLAUDE.md` é a fonte oficial de padrões técnicos, não de regra de negócio — não usar
  `CLAUDE.md` para decidir uma regra. É mantido pelo `/architect`, não por esta skill.
- Se `PROJECT_CONTEXT.md` e `CLAUDE.md` parecerem se contradizer sobre algo (ex:
  `PROJECT_CONTEXT.md` descreve uma regra de domínio que implicaria uma mudança de
  arquitetura documentada em `CLAUDE.md` de um jeito incompatível), **não escolha um dos
  dois silenciosamente** — sinalize o conflito explicitamente ao usuário e pare até ele
  decidir.
- Se o código atual diverge do que `PROJECT_CONTEXT.md` descreve como regra esperada,
  isso não é necessariamente um "conflito de fontes" — na maioria dos casos é uma lacuna
  já conhecida (seção 9). Trate como lacuna, não como erro, a menos que a divergência não
  esteja catalogada ali.

## Como responder a uma pergunta de domínio

1. Identifique a entidade/regra envolvida (Cliente, Barbeiro, ServicoDesejado,
   Agendamento, disponibilidade, status, etc.).
2. Busque em `PROJECT_CONTEXT.md` (seções 7–11) se a regra já está documentada.
3. Se necessário, confira o código atual (`model/`, `service/`) para saber o estado real
   de implementação — sem tratar o código como autoritativo sobre a intenção de negócio.
4. Responda em um formato claro com o estado da regra:
   - ✅ **Confirmada e implementada** — descreva a regra e onde ela vive no código.
   - ⚠️ **Confirmada, mas não implementada (lacuna conhecida)** — descreva a regra
     esperada e o que falta, referenciando a seção 9 do `PROJECT_CONTEXT.md`.
   - ❓ **Não definida** — a pergunta não tem resposta em `PROJECT_CONTEXT.md` nem foi
     discutida antes. Não invente a regra: pergunte ao usuário o suficiente para definir
     o comportamento esperado.
   - 🔴 **Conflito de fontes** — descreva o conflito encontrado entre `PROJECT_CONTEXT.md`
     e `CLAUDE.md` (ou entre `PROJECT_CONTEXT.md` e o código, se for grave o bastante
     para não ser tratado como lacuna comum) e pare para o usuário decidir.
5. **Atualize `PROJECT_CONTEXT.md` sempre que uma dessas duas situações acontecer** — não
   deixe a decisão só na conversa:
   - **Pergunta ❓ respondida pelo usuário** → vira uma decisão de negócio nova. Registre-a
     na seção mais adequada (7, 8, 10 ou 11).
   - **Lacuna ⚠️ da seção 9 foi fechada** — tipicamente porque `/feature` acabou de
     implementar algo que antes era lacuna conhecida e consultou `/domain` para registrar
     isso. Nesse caso, mova o item da seção 9 para a seção 8 (regras já implementadas),
     removendo-o da lista de lacunas.

   Em ambos os casos, informe quem consultou (usuário ou a skill que chamou) que o
   documento foi atualizado.

## Quando usar

- Sempre que `/architect` identificar que uma solicitação impacta regra de negócio.
- Quando `/feature`, `/test`, `/review` ou `/refactor` precisarem confirmar como uma
  regra deve se comportar antes de agir.
- Quando `/feature` fechar uma lacuna da seção 9 e precisar registrar a atualização em
  `PROJECT_CONTEXT.md`.
- Quando o usuário quiser só entender/discutir uma regra, sem necessariamente implementar
  nada ainda.

## Quando NÃO usar

- Para mudanças puramente técnicas sem impacto de negócio (ex: extrair um método,
  ajustar um nome de variável) — isso nem passa por aqui, fica em `/architect`/`/refactor`.
- Para efetivamente implementar, testar, revisar ou documentar algo — isso é sempre outra
  skill, mesmo que a regra já esteja clara.
