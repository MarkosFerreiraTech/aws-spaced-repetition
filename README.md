# AWS Simulado Espaçado

> Cole o simulado. Estude só o que você está prestes a esquecer.

Ferramenta web que transforma simulados de certificação AWS (texto bruto da Udemy)
em uma fila de estudo com **repetição espaçada (SM-2)**.

Projeto de MVP da formação full stack — desenvolvimento assistido por IA, com foco em
engenharia de software: aquitetura em camadas, SOLID, testes automatizados, portões de
qualidade, DevSecOps e análise de desempenho.

**Status:** Dia 1 concluído (núcleo do backend) — ver [relatório](docs/day-1-report.md).

---

## Índice

- [O problema](#o-problema)
- [Arquitetura](#arquitetura)
- [Como rodar](#como-rodar)
- [API](#api)
- [Qualidade](#qualidade)
- [Decisões de engenharia](#decisões-de-engenharia)
- [Roadmap](#roadmap)

---

## O problema

Refazer simulados "no feeling" não funciona: você revisa o que já domina e esquece o
que não domina. Faltava um sistema que calculasse **quando** cada questão deve voltar —
e uma forma de alimentar esse sistema sem digitar 65 questões à mão.

Duas peças:

1. **Importador Inteligente** (Dia 2): cola o texto bagunçado → LLM (Gemini) estrutura
   pergunta/alternativas/explicação → **tela de revisão humana** → banco.
2. **Motor de repetição espaçada** (Dia 1, ✅): calcula o próximo vencimento de cada
   questão a partir do desempenho.

---

## Arquitetura

Monorepo com front e back separados (ADR-003 do escopo):

```
aws-spaced-repetition/
├── backend/            Spring Boot 3.3 · Java 21 · Postgres 16 · Flyway
├── frontend/           Angular + Clarity (Dia 2)
├── docs/               SPEC, ADRs, relatórios
└── docker-compose.yml  Postgres local
```

### Backend: camadas com dependências apontando para dentro

```
        api/                 controllers, DTOs, mappers, Problem Details
         │  (depende de)
         ▼
    application/            casos de uso (ReviewService), comandos, views
         │
         ▼
      domain/               modelos ricos, regras, Scheduler, portas  ← sem framework
         ▲
         │  (implementa as portas)
    infrastructure/         JPA, adapters, security, config
```

Regras verificadas por **ArchUnit** (`ArchitectureTest`), não apenas por convenção:

| Regra | Por quê |
|---|---|
| `domain` não importa Spring nem Jakarta | o coração do produto é testável sem container |
| `application` não conhece `infrastructure` | troca de banco/provedor de LLM não afeta caso de uso |
| `api` não conhece `persistence` | nenhum controller acopla a detalhe de banco |
| Controllers só em `api.controller` | previsibilidade de navegação |

```
com.spacedrep
├── api/            controller · dto · mapper · error
├── application/    ReviewService · CurrentUserProvider · command · view
├── domain/         model · scheduler · port · exception
└── infrastructure/ persistence (entity · repository · adapter) · security · config
```

### O algoritmo (SM-2)

`Scheduler` é uma **interface**; `Sm2Scheduler` é a implementação atual. Trocar por FSRS
(roadmap M2) é trocar um bean.

```
EF' = EF + (0.1 − (5 − q) × (0.08 + (5 − q) × 0.02))     piso em 1.30
q < 3  → lapse: repetitions = 0, intervalo = 1 dia
q ≥ 3  → repetitions += 1
         intervalo = 1                          (1ª vez)
                   = 6                          (2ª vez)
                   = round(intervalo × EF')     (3ª vez em diante)
```

Mapeamento resposta → qualidade (regra de negócio, testada em `ReviewQualityTest`):

| Situação | q |
|---|---|
| errou (resposta única) | 2 |
| errou parcialmente em multi-resposta (*Choose TWO*) | 3 |
| acertou com dificuldade / levou > 60s | 3 |
| acertou | 4 |
| acertou em < 15s | 5 |

---

## Como rodar

### Pré-requisitos
- JDK 21+, Maven 3.9+, Docker (ou um Postgres local)

### 1. Banco

```bash
cp .env.example .env
docker compose up -d postgres
```

### 2. Backend

```bash
cd backend
./mvnw spring-boot:run     # ou: mvn spring-boot:run
```

Flyway aplica `V1__init.sql` (schema) e `V2__seed.sql` (dados de demonstração).

### 3. Testar de verdade

```bash
curl http://localhost:8080/api/reviews/due?limit=3

curl -X POST http://localhost:8080/api/reviews/1/answer \
  -H 'Content-Type: application/json' \
  -d '{"selectedOptionIds":[2],"durationMs":8000}'
```

---

## API

Base `/api` · erros no padrão **RFC 9457** (`application/problem+json`)

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/reviews/due?limit=20` | fila do dia (1..100) |
| POST | `/api/reviews/{cardId}/answer` | responder e reagendar |
| GET | `/actuator/health` | health check |

**Contrato de segurança:** a fila do dia **não** expõe quais alternativas estão corretas —
o cliente só descobre ao responder. Há teste de integração garantindo isso.

| Código | Quando |
|---|---|
| 400 | payload inválido / alternativa de outra questão |
| 403 | card de outro usuário |
| 404 | card ou questão inexistente |
| 500 | erro inesperado (sem vazamento de stacktrace) |

---

## Qualidade

| Métrica | Valor (Dia 1) | Gate |
|---|---|---|
| Testes unitários | 36 | — |
| Testes de integração | 5 (Postgres real) | — |
| Cobertura de linhas | **91,3%** | ≥ 70% |
| Cobertura `domain.scheduler` | 100% | ≥ 85% |
| Regras arquiteturais | 4 | 100% verdes |

```bash
mvn verify -Pcoverage-gate                    # com Docker
# sem Docker (usa Postgres local):
TEST_DB_URL=jdbc:postgresql://localhost:5432/spacedrep \
TEST_DB_USER=spacedrep TEST_DB_PASSWORD=spacedrep mvn verify -Pcoverage-gate
```

Desempenho medido (ver [docs/performance.md](docs/performance.md)):
`GET /api/reviews/due` p95 = **25,5 ms** (meta: < 200 ms).

---

## Decisões de engenharia

Todas em [docs/adr/](docs/adr/). As que mais importam:

| ADR | Decisão |
|---|---|
| [001](docs/adr/ADR-001-sonarcloud.md) | SonarCloud em vez de SonarQube self-hosted |
| [002](docs/adr/ADR-002-authelia-adiada.md) | JWT no Spring Security; Authelia no M2 |
| [003](docs/adr/ADR-003-trunk-based.md) | Trunk-based + Conventional Commits em vez de Gitflow completo |
| [004](docs/adr/ADR-004-import-duas-etapas.md) | Import em 2 etapas (parse → revisão → confirm) |
| [005](docs/adr/ADR-005-token-storage.md) | Token em memória/`sessionStorage` (dívida declarada) |

### Segurança (postura do Dia 1)

- Domínio sem dependência de framework → nada de `@Entity` vazando para a API.
- Entidades JPA **nunca** serializadas: DTOs explícitos (`CardResponse`, `AnswerResponse`).
- Erros sem stacktrace e sem SQL expostos ao cliente.
- Query de fila sempre filtrada por `user_id`; posse do card validada no serviço.
- Nenhum segredo no código: tudo por variável de ambiente (`.env.example` versionado, `.env` não).
- ⚠️ **Dívida consciente:** endpoints estão liberados até o Dia 2 (JWT). Está marcado com
  `TODO(Dia 2)` em `SecurityConfig`.

---

## Roadmap

| M0 — 3 dias (atual) | M1 | M2 |
|---|---|---|
| ✅ SM-2 + testes | Importador Gemini + tela de revisão | Authelia + reverse proxy |
| ✅ fila do dia + resposta | JWT + Spring Security | FSRS no lugar de SM-2 |
| Frontend Angular + Clarity | Deploy staging + DAST (ZAP) | Multi-usuário aberto |
| Quality gate no CI | k6 + análise de carga | BFF (token fora do browser) |

---

## Licença / uso

Projeto pessoal de estudo. As questões importadas são de uso individual — o sistema não
redistribui conteúdo de terceiros.
