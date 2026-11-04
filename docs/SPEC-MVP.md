# Spec de Engenharia — MVP "AWS Simulado Espaçado"

**Versão:** 1.0 · **Data:** 2026-11-04 · **Prazo de entrega:** D+3 (2026-11-07)
**Autor:** (você) · **Status:** aprovada para execução (escopo M0)

---

## 0. Sumário executivo

Ferramenta web para refazer simulados de certificação AWS usando **repetição espaçada**, alimentada por um **importador inteligente** (texto bruto da Udemy → LLM → questões estruturadas).

Em 3 dias, entregamos o **M0**: um produto funcional de ponta a ponta (importar → revisar → estudar → agendar), com pipeline de qualidade real. Tudo que não couber com qualidade fica **explicitamente fora do escopo, com ADR justificando** — não fica "quase pronto".

> **Regra de ouro desta entrega:** um fluxo completo e honesto vale mais que cinco fluxos pela metade.

### Corte de escopo (leia antes de tudo)

| Item pedido | M0 (3 dias) | Justificativa |
|---|---|---|
| Repetição espaçada | ✅ SM-2 completo | É o produto |
| Importador LLM (Gemini) | ✅ com tela de revisão | É o diferencial |
| Backend Spring Boot em camadas | ✅ | Inegociável |
| Testes automatizados | ✅ núcleo (ver §9) | Inegociável |
| Portões de qualidade | ✅ via **SonarCloud** | SonarQube self-hosted não cabe em 3 dias |
| CI/CD GitHub Actions | ✅ build + testes + gates | Deploy automatizado fica em M1 |
| Git + commits atômicos + semver | ✅ **trunk-based simplificado** | Gitflow completo não cabe (ADR-003) |
| JWT em todas as transações | ✅ Spring Security | Authelia fica para M2 (ADR-002) |
| **Authelia** | ❌ M2 | Exige reverse proxy + backend de usuários |
| Frontend Angular + Clarity | ✅ 4 telas | Telas de admin ficam fora |
| DevSecOps | ✅ secrets, gitleaks, Dependabot, validação de input | DAST/ZAP fica em M1 |
| Análise de desempenho | ✅ medição documentada | Teste de carga k6 é opcional (M1) |

### Premissas a validar (marcadas com ⚠️ no documento)

1. ⚠️ **Single-user** com `user_id` no modelo desde já.
2. ⚠️ Texto da Udemy contém: enunciado, alternativas, resposta correta e explicação. **Sem imagens.**
3. ⚠️ Questões de múltipla resposta ("Choose TWO") existem e precisam ser suportadas.
4. ⚠️ Repositório **público** no GitHub (requisito para SonarCloud gratuito).
5. ⚠️ A conta Gemini é free tier e os limites atuais foram conferidos antes do dia 1.

---

## 1. Problema e usuário

**Problema:** refazer simulados de AWS de forma intuitiva ("vou refazer o que errei") não funciona — sem agendamento, o estudante revisa o que já sabe e esquece o que não sabe.

**Usuário:** candidato a certificação AWS que já tem simulados em texto (Udemy) e quer transformá-los em uma fila de estudo diária.

**Proposta de valor:** *"Cole o simulado. Estude só o que você está prestes a esquecer."*

**Métrica de sucesso do MVP:** importar 1 simulado de 65 questões em < 2 minutos e ter a fila do dia pronta.

---

## 2. Escopo funcional do M0

### F1 — Autenticação
- Login com e-mail + senha → JWT (HS256). Toda rota `/api/**` exige token, exceto `/api/auth/login`.
- Senha com BCrypt (custo 10+). Sem cadastro público no M0: usuário semeado via migration (`seed`) ou endpoint de registro protegido.

### F2 — Importador Inteligente
Fluxo de **duas etapas** (decisão central de design, ADR-004):

1. `POST /api/imports/parse` → recebe texto bruto, chama Gemini, **não persiste**, devolve rascunho estruturado.
2. Usuário revisa/edita na tela (corrige o que o LLM errou).
3. `POST /api/imports/confirm` → persiste as questões revisadas.

Por que não persistir direto: o LLM vai errar (fundir questões, perder a correta, alucinar explicação). Gravar sem revisão contamina a base de estudo de forma silenciosa.

Regras:
- Limite de payload: **100.000 caracteres** (HTTP 413 acima disso).
- Timeout da chamada ao Gemini: **45s**; 1 retry em erro 5xx/429.
- Saída validada contra schema; se o JSON não validar após retry → HTTP 422 com erro descritivo.
- Deduplicação: `sha256(raw_text)` em `exam_source`; reimportar o mesmo texto retorna 409.
- Rate limit no endpoint: **5 requisições/minuto por usuário** (proteção de custo/cota).

### F3 — Sessão de estudo
- `GET /api/reviews/due?limit=20` → fila do dia ordenada por `due_date`.
- Responder: `POST /api/reviews/{cardId}/answer` com alternativas selecionadas + tempo de resposta → retorna o agendamento recalculado.
- Feedback imediato: correta/incorreta + explicação.
- Fim da fila: tela de resumo da sessão.

### F4 — Painel
- Total de questões, dominadas (intervalo ≥ 21 dias), pendentes hoje, taxa de acerto dos últimos 7 dias, próximos 7 dias de carga.

### Fora de escopo (roadmap)

| M1 | M2 |
|---|---|
| Deploy automatizado (staging) + DAST ZAP | Authelia + reverse proxy + LLDAP |
| k6 + análise de carga formal | FSRS no lugar de SM-2 |
| Edição/exclusão de questões pós-import | Multi-usuário aberto (cadastro) |
| Import de imagens (OCR/upload) | App mobile / PWA offline |

---

## 3. Modelo de domínio e banco

PostgreSQL 16 · migrations com **Flyway** (`V1__init.sql`, `V2__seed.sql`).

```
user_account (id, email UNIQUE, password_hash, display_name, created_at)

exam_source (id, user_id FK, name, provider, raw_text_hash CHAR(64) UNIQUE,
             imported_at)                                  -- dedup do import

question (id, exam_source_id FK, topic, stem TEXT,
          explanation TEXT, multi_select BOOLEAN,
          created_at)

answer_option (id, question_id FK, position SMALLINT, text TEXT,
               is_correct BOOLEAN)                          -- N corretas permitidas
          UNIQUE (question_id, position)

review_card (id, user_id FK, question_id FK,                 -- 1 card por user+question
             ease_factor NUMERIC(4,2) DEFAULT 2.50,
             interval_days INT DEFAULT 0,
             repetitions INT DEFAULT 0,
             due_date DATE NOT NULL,
             last_reviewed_at TIMESTAMPTZ,
             UNIQUE (user_id, question_id))

review_attempt (id, card_id FK, quality SMALLINT,            -- 0..5 (escala SM-2)
                is_correct BOOLEAN,
                duration_ms INT,
                answered_at TIMESTAMPTZ)
```

Índices obrigatórios: `idx_review_card_due (user_id, due_date)`, `idx_question_source (exam_source_id)`.

**Regra de negócio no banco:** `ease_factor >= 1.30` (CHECK), `interval_days >= 0` (CHECK), `is_correct` coerente com `quality` (validado no serviço, testado).

---

## 4. Algoritmo de repetição espaçada (SM-2)

Interface `Scheduler` no domínio, implementação `Sm2Scheduler` — permite trocar por FSRS em M2 sem tocar em serviços (Dependency Inversion).

```
DADO: card (ef, interval, repetitions), resposta do usuário

1. quality q ∈ {0..5}  ← mapeamento explícito:
   - errou                                    → q = 2
   - acertou, tempo > 60s ou multi-select parcial → q = 3
   - acertou                                  → q = 4
   - acertou em < 15s                          → q = 5
   (limitado ao máximo de 5)

2. EF' = EF + (0.1 − (5 − q) × (0.08 + (5 − q) × 0.02))
   EF' = max(1.30, EF')

3. SE q < 3  (lapse):
       repetitions = 0
       interval    = 1
   SENÃO:
       repetitions = repetitions + 1
       interval = 1                    se repetitions == 1
                = 6                    se repetitions == 2
                = round(interval × EF') se repetitions >= 3

4. due_date = hoje + interval
5. Retorna card atualizado
```

**Casos de teste obrigatórios (TDD aqui, é o coração):**
- primeira resposta correta → interval 1, repetitions 1, EF 2.60
- segunda correta → interval 6
- terceira correta → interval = round(6 × EF)
- erro após sequência longa → repetitions 0, interval 1, EF reduzido mas ≥ 1.30
- 10 erros consecutivos → EF nunca abaixo de 1.30 (não "explode" nem zera)
- `q=3` não zera a repetição (é acerto fraco, não lapse)

**Não decidido no M0:** fila diária com tamanho máximo é escolhida pelo usuário (`limit` na query), não imposta pelo sistema.

---

## 5. Contrato da API

Base: `/api` · Auth: `Authorization: Bearer <jwt>` · Erros: RFC 9457 `application/problem+json`

| Método | Rota | Corpo | Resposta |
|---|---|---|---|
| POST | `/api/auth/login` | `{email, password}` | `201 {token, expiresAt}` |
| POST | `/api/imports/parse` | `{sourceName, rawText}` | `200 {questions:[draft]}` |
| POST | `/api/imports/confirm` | `{sourceName, rawTextHash, questions:[...]}` | `201 {examSourceId, createdCount}` |
| GET | `/api/reviews/due?limit=20` | — | `200 {cards:[...], remaining}` |
| POST | `/api/reviews/{cardId}/answer` | `{selectedOptionIds[], durationMs}` | `200 {isCorrect, explanation, nextDueDate, card}` |
| GET | `/api/stats/summary` | — | `200 {total, mastered, dueToday, accuracy7d, upcoming[]}` |

Códigos: `400` validação · `401` token inválido/ausente · `403` recurso de outro usuário · `409` import duplicado · `413` texto grande demais · `422` LLM não estruturável · `429` rate limit.

**Draft de questão** (usado em parse/confirm):
```json
{
  "stem": "...",
  "topic": "IAM",
  "multiSelect": false,
  "explanation": "...",
  "options": [
    {"position": 1, "text": "...", "correct": true},
    {"position": 2, "text": "...", "correct": false}
  ]
}
```

---

## 6. Arquitetura do backend

Java 21 · Spring Boot 3.x · Maven · PostgreSQL · Flyway · Spring Security · Spring Data JPA · Bean Validation · Lombok (opcional, justificar) · springdoc-openapi.

```
com.seuorg.spaced
├── api/              # controllers, DTOs de entrada/saída, mappers, exception handler
│   ├── controller/
│   ├── dto/
│   └── error/        # @RestControllerAdvice → ProblemDetail
├── application/      # serviços de caso de uso (@Service, @Transactional)
│   ├── scheduler/    # Scheduler (interface) + Sm2Scheduler
│   └── import/       # ImportService, LlmPort (interface)
├── domain/           # entidades de domínio + regras (sem anotação de framework)
├── infrastructure/    # JPA repositories, GeminiClient (implements LlmPort), security
│   ├── persistence/
│   ├── llm/
│   └── security/     # JwtService, JwtAuthFilter, SecurityConfig
└── config/
```

Regras arquiteturais (verificáveis em code review):
- `api` depende de `application`; `application` depende de `domain`; `infrastructure` implementa portas. **Nenhuma dependência invertida.**
- `domain` não importa `jakarta.persistence.*` nem `org.springframework.*`.
- Controller não contém regra de negócio: só valida, converte e delega.
- **DTO ≠ Entity:** entidades JPA nunca são serializadas na resposta.
- Chamada ao Gemini atrás de `LlmPort` (`interface`), implementada por `GeminiClient` em `infrastructure` → testável com mock, sem chamar a API real.
- `SecurityConfig` com `SecurityFilterChain`, `stateless`, CSRF desabilitado (API pura), CORS restrito à origem do front.

---

## 7. Frontend

Angular (versão estável mais recente **compatível com Clarity** — se o `ng add @clr/angular` não suportar a versão, faça downgrade do Angular; isso é um risco real, ver §12) + `@cds/core` + `@clr/angular`, standalone components, signals onde fizer sentido.

| Tela | Componentes Clarity |
|---|---|
| Login | `clr-input-container`, `clr-password-container`, `clr-button` |
| Importar | `clr-textarea-container` (contador de caracteres), `clr-datagrid` para revisão editável, `clr-alert` para erros do parse |
| Estudar | `clr-radio-wrapper` / `clr-checkbox-wrapper` (multi-select), `clr-progress-bar`, feedback com `clr-alert` |
| Painel | `clr-cards` + `clr-datagrid` de próximos vencimentos |

- Interceptor HTTP injetando o Bearer token; `401` → volta ao login.
- Token guardado **em memória + `sessionStorage`** (⚠️ decisão consciente: `localStorage` é mais vulnerável a XSS; cookie HttpOnly exigiria CSRF no backend e não cabe em 3 dias — ADR-005).
- Serviço de estado com signals; sem NgRx (over-engineering para o escopo).
- Acessibilidade: toda grid/input do Clarity com `clrForm`/labels; foco visível; contraste — Clarity já entrega, não sobrescrever CSS sem motivo.

---

## 8. Segurança e DevSecOps

| Controle | M0 | Onde |
|---|---|---|
| API Key do Gemini fora do código | ✅ | `application.yml` lê `${GEMINI_API_KEY}`; `.env` gitignored; `.env.example` versionado |
| Secrets no CI | ✅ | GitHub Secrets / Environments |
| Secret scanning no pipeline | ✅ | `gitleaks` (bloqueia merge) |
| Dependências vulneráveis | ✅ | Dependabot (todos os ecossistemas) |
| Validação de entrada | ✅ | Bean Validation + limite de tamanho |
| SQL injection | ✅ | JPA/Prepared Statements, zero concatenação |
| Isolamento por usuário | ✅ | toda query filtra por `user_id` do token (teste dedicado) |
| Log sem dados sensíveis | ✅ | nunca logar senha, token, API key ou texto bruto do simulado |
| **Prompt injection** | ✅ mitigado | texto colado = untrusted: delimitado no prompt, instrução de sistema fixa, saída validada por schema, nunca executada como instrução |
| Rate limiting | ✅ | `/imports/parse`: 5/min/usuário |
| DAST (ZAP) | ⛔ M1 | exige ambiente deployado |
| Pentest / SAST dedicado | ⛔ M1 | SonarCloud cobre parcialmente |

**Autenticação (ADR-002):** Spring Security como *resource server* próprio (JWT HS256 de 8h, sem refresh no M0). Authelia fica em M2 — ela não é uma lib, é um portal SSO atrás de reverse proxy com backend de usuários (arquivo/LLDAP/LDAP); montar isso em 3 dias consumiria o tempo do produto.

> ⚠️ **Requisito reformulado:** "somente o frontend pode chamar a API" é inatingível em SPA — o usuário abre o DevTools e copia o próprio token, e CORS é política de browser, não controle de acesso. O requisito real é **"somente usuário autenticado"**. Se algum dia você quiser de fato esconder o token do browser, o padrão é **BFF**. Isso está registrado para a banca/facilitador não achar que foi esquecimento.

---

## 9. Estratégia de teste e portões de qualidade

### Backend
| Nível | Ferramenta | Alvo |
|---|---|---|
| Unitário — scheduler | JUnit 5 + AssertJ | 100% dos casos da §4 |
| Unitário — serviços | JUnit 5 + Mockito | regras de negócio, isolamento por usuário |
| Integração — API | `@SpringBootTest` + MockMvc + Testcontainers Postgres | login, parse (WireMock), confirm, due, answer |
| LLM simulado | **WireMock** | resposta válida, JSON malformado, timeout, 429 |
| Arquitetura (opcional, alto valor) | ArchUnit | garantir as regras da §6 |

**Cobertura:** JaCoCo ≥ **70% global** e ≥ **85%** nos pacotes `application.scheduler` e `application.import`. Justificativa para 70% e não 80%: em 3 dias, perseguir 80% global produz teste de getter, que é pior que cobertura menor com teste de regra.

### Frontend
- 2 testes unitários de valor real (parse de draft, cálculo de seleção) — não testar componente de UI trivial.
- Checklist manual E2E documentado em `docs/manual-test-plan.md` (5 passos).

### Quality Gate (SonarCloud — ADR-001)
- `new_coverage ≥ 70%`
- `new_bugs = 0`, `new_vulnerabilities = 0`, `new_code_smells = 0`
- `new_duplicated_lines_density ≤ 3%`
- Security Hotspots: 100% revisados
- O gate **bloqueia merge** (required check na branch `main`).

---

## 10. Git e CI/CD

**Fluxo (ADR-003):** trunk-based simplificado — `main` protegida, branches `feat/*`, `fix/*`, `docs/*` com vida ≤ 1 dia, PR obrigatório, squash merge.
**Commits:** Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `chore:`), atômicos, imperativo em inglês.
**Versionamento:** SemVer por tag manual (`v0.1.0` no fim do M0); tag `v0.1.0` no MVP funcional.
**Gitflow completo:** fora — `develop/release/hotfix` em projeto solo de 3 dias é cerimônia sem benefício (o próprio autor do Gitflow desaconselha para entrega contínua).

### Pipeline `.github/workflows/ci.yml`
```
pull_request → main
  backend:  setup-java 21 + cache m2 → mvn -B verify (unit + integ + JaCoCo)
            → gitleaks → sonar scan + quality gate
  frontend: setup-node → npm ci → lint → build → npm test -- --watch=false
  required checks: backend-tests, frontend-build, gitleaks, sonar-gate
```
Deploy automatizado: **M1** (documentado, não implementado).

---

## 11. Análise de desempenho

Medição obrigatória, documentada em `docs/performance.md` com tabela real (não estimativa):

| Endpoint | Métrica | Meta M0 |
|---|---|---|
| `GET /reviews/due` | p95 (100 itens) | < 200 ms |
| `POST /reviews/{id}/answer` | p95 | < 250 ms |
| `POST /stats/summary` | p95 | < 300 ms |
| `POST /imports/parse` | p95 (40 questões) | < 30 s — **limitado pelo LLM, medir separado e explicitar** |

Método: 50 requisições sequenciais com `curl -w` → tabela com média/p95/máx; ou script k6 se sobrar tempo. Índice `(user_id, due_date)` é o que sustenta a meta de `due`.

**Observabilidade mínima:** logs estruturados JSON, `X-Request-Id` por requisição, tempo da chamada ao Gemini logado, Actuator (`/actuator/health` público, resto protegido).

---

## 12. Plano de 3 dias

**Dia 1 — Fundação (backend rodando de ponta a ponta, sem LLM e sem JWT)**
1. Repo, `.gitignore`, `.env.example`, README inicial, docker-compose com Postgres.
2. Maven + Spring Boot + Flyway ✅ migrations ✅ seed.
3. Domínio + entidades + repositórios.
4. `Sm2Scheduler` **com testes primeiro** (é o ativo mais valioso — se o tempo acabar, isso está pronto).
5. Endpoints `due` e `answer` funcionando (com `permitAll` temporário).
6. Commit ao fim de cada bloco. Tag `v0.1.0-alpha`.

**Dia 2 — Importador + Segurança + Frontend base**
1. `LlmPort` + `GeminiClient` + prompt + validação de schema.
2. `POST /imports/parse` + `confirm` com testes WireMock.
3. Spring Security + JWT + `SecurityConfig` + filtro + testes de 401/403.
4. Angular + Clarity instalados (⚠️ checar compatibilidade cedo, no início do dia — não no fim).
5. Telas Login + Importar (parse → revisão → confirmar).
6. Tag `v0.1.0-beta`.

**Dia 3 — Telas de estudo, qualidade e entrega**
1. Telas Estudar + Painel.
2. CI completo: workflow, gitleaks, SonarCloud, branch protection.
3. Rodar quality gate, corrigir o que bloquear (reserve 3h — sempre bloqueia algo).
4. `docs/performance.md` com medições reais + `manual-test-plan.md`.
5. README com arquitetura, diagrama, como rodar, decisões.
6. ADRs finalizados. Tag `v0.1.0`. Gravar demo de 3 min.

**Buffer:** o Dia 2 é o mais perigoso (Clarity + Gemini + JWT no mesmo dia). Se atrasar, **corte a tela Painel** (F4) — ela é a única feature dispensável sem quebrar o fluxo.

---

## 13. Definition of Done (M0)

O MVP está pronto quando, em uma máquina limpa:
1. `docker compose up` sobe o Postgres e o backend; `npm start` sobe o front.
2. Login funciona e rota sem token retorna 401.
3. Colar um simulado real de ≥ 20 questões produz rascunho válido, permite revisão e persiste.
4. Reimportar o mesmo texto retorna 409.
5. A fila do dia exibe as questões pendentes; responder acerta/erra e recalcula `due_date` conforme §4.
6. Errar uma questão a faz voltar ao intervalo 1 (comportamento verificável na tela).
7. `mvn verify` verde; cobertura ≥ 70%; SonarCloud gate verde no PR.
8. Nenhum segredo no histórico do git (`gitleaks` limpo).
9. README explica a arquitetura e como rodar; 5 ADRs registram os cortes.

---

## 14. Riscos

| # | Risco | Prob. | Impacto | Mitigação |
|---|---|---|---|---|
| R1 | Clarity incompatível com a versão do Angular | Média | Alto | Testar `ng add @clr/angular` no **primeiro** dia, versão do Angular fixada |
| R2 | Gemini free tier estoura cota / muda de modelo | Média | Alto | Conferir limites no dia 1; `LlmPort` permite trocar de provedor; WireMock nos testes para não gastar cota |
| R3 | LLM devolve JSON inválido com frequência | Alta | Médio | `responseSchema` + 1 retry + tela de revisão como rede de segurança |
| R4 | Quality gate bloqueia na véspera | Média | Alto | Configurar SonarCloud no **dia 2**, não no dia 3 |
| R5 | Escopo cresce ("só mais um campo") | Alta | Alto | Este documento é o contrato; ideias novas vão para o roadmap |
| R6 | Authelia cobrada como requisito obrigatório | Baixa | Médio | ADR-002 escrito para defender a decisão com argumento técnico |

---

## 15. ADRs

- **ADR-001** — SonarCloud em vez de SonarQube self-hosted
- **ADR-002** — JWT próprio no Spring Security; Authelia adiada para M2
- **ADR-003** — Trunk-based simplificado em vez de Gitflow completo
- **ADR-004** — Import em duas etapas (parse → revisão → confirm) em vez de gravação direta
- **ADR-005** — Token em memória/`sessionStorage` em vez de cookie HttpOnly (e o que isso custa)

*(Arquivos individuais em `docs/adr/`.)*
