# AvantBarber

Sistema de gestão para uma barbearia real (single-tenant), com API backend em Spring Boot e uma landing page institucional. Hoje o agendamento em si acontece pelo WhatsApp — o barbeiro registra o atendimento no sistema; a evolução planejada é automatizar esse fluxo via n8n.

## Visão geral

- **Hoje:** o cliente combina o horário com a barbearia pelo WhatsApp, e o barbeiro confirma e registra o agendamento manualmente no sistema. A landing page é só institucional: apresenta a barbearia e os serviços, e direciona o cliente para o WhatsApp — ela não agenda nada.
- **Em construção:** integração com [n8n](https://n8n.io/) para que agendamentos criados a partir da conversa no WhatsApp sejam automaticamente validados e persistidos pela API (nascendo como `PENDENTE`, com origem `AUTOMACAO` e limite de pendentes simultâneos por cliente).
- **Futuro:** painel administrativo para gestão da barbearia.

A API é a fonte única de verdade de todas as regras de negócio — nunca as automações externas ou os clientes (landing page, painel).

## Estrutura do repositório

```
avant/       Backend Spring Boot (Maven) — todo o código Java vive aqui
front-end/   Landing page institucional (HTML/CSS/JS puro, sem build)
postman/     Coleção Postman com os contratos da API
```

Não há build file na raiz — comandos Maven sempre rodam de dentro de `avant/`.

## Stack

- **Backend:** Java 25, Spring Boot 4, Spring Data JPA, Spring Security (OAuth2 login com Google), Bean Validation, Lombok
- **Banco de dados:** PostgreSQL (schema derivado das entidades JPA, `ddl-auto: update`, sem migrations)
- **Front-end:** HTML/CSS/JS estático, sem framework
- **CI:** GitHub Actions (`.github/workflows/pipeline.yaml`), build e testes com Postgres em container de serviço

## Arquitetura do backend

Camadas clássicas por tipo, em `com.avantbarber.avant`:

```
controller/  endpoints REST (JSON in/out), um por agregado
service/     regras de negócio, orquestra repositórios, mapeia entidade <-> DTO
repository/  interfaces Spring Data JPA
model/       entidades JPA
dto/         DTOs de request/response (services nunca retornam entidades para controllers)
exception/   exceções de domínio
infra/       tratamento central de exceções (RestExceptionHandler)
config/      segurança, OAuth2, CORS
```

Domínio principal: `Cliente`, `Barbeiro` (com perfil ADMIN/BARBEIRO), `ServicoDesejado` e `Agendamento` (que liga os três com data/hora e status). `AgendamentoService` concentra as regras de negócio de agendamento: horário de funcionamento por dia da semana, bloqueio de horários no passado, prevenção de conflito de horário e cálculo de slots disponíveis.

## Rodando localmente

Pré-requisitos: JDK 25, PostgreSQL acessível localmente (banco `avant_barbearia`).

Variáveis de ambiente obrigatórias:

| Variável | Descrição | Default |
|---|---|---|
| `DB_URL` | URL JDBC do Postgres | `jdbc:postgresql://localhost:5432/avant_barbearia` |
| `DB_USERNAME` | Usuário do banco | `postgres` |
| `DB_PASSWORD` | Senha do banco | — (obrigatório) |
| `GOOGLE_CLIENT_ID` | Client ID do OAuth2 Google | — (obrigatório) |
| `GOOGLE_CLIENT_SECRET` | Client Secret do OAuth2 Google | — (obrigatório) |

A partir de `avant/`:

```bash
# build + testes
./mvnw.cmd clean package        # Windows
./mvnw clean package            # Unix

# rodar só os testes
./mvnw.cmd test

# subir a aplicação
./mvnw.cmd spring-boot:run
```

A landing page em `front-end/` é estática — basta abrir `index.html` no navegador (ela consome os endpoints públicos da API rodando em `localhost:8080`).

## Autenticação e acesso

Login via OAuth2 do Google (`Barbeiro`/`Admin`). Dois endpoints públicos, sem autenticação, servem a landing page:

- `GET /barbeiros/publico` — lista de barbeiros (id + nome, nunca CPF/telefone)
- `GET /servicos-desejados/publico` — lista de serviços oferecidos

Todos os demais endpoints exigem autenticação.

## Status do projeto

Em desenvolvimento ativo, uso real por uma barbearia. O foco atual é consolidar o core de negócio (regras de agendamento) antes de expandir a automação via n8n e o painel administrativo.
