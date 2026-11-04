# Relatório — Dia 1 (fechamento)

**Data:** 2026-11-04 · **Escopo da SPEC:** fundação do backend (seções 3, 4, 5, 6, 9)

## Entregue

| Item | Status | Evidência |
|---|---|---|
| Estrutura do monorepo | ✅ | `backend/`, `frontend/`, `docs/`, `docker-compose.yml` |
| Schema versionado (Flyway) | ✅ | `V1__init.sql`, `V2__seed.sql` — aplicados em Postgres 17 real |
| Modelo de domínio sem framework | ✅ | `domain/model`, `domain/scheduler` — ArchUnit verde |
| SM-2 com TDD | ✅ | 17 testes no scheduler/qualidade, cobertura 100% no pacote |
| Fila do dia + resposta | ✅ | `GET /api/reviews/due`, `POST /api/reviews/{id}/answer` |
| Separação DTO × Entity | ✅ | `api/dto` + adapters; entidades JPA não serializadas |
| Erros RFC 9457 | ✅ | 400/403/404 com `application/problem+json` |
| Testes unitários | ✅ | 36 testes |
| Testes de integração | ✅ | 5 testes contra Postgres real |
| Gate de cobertura | ✅ | 91,3% (mínimo 70%) |
| Baseline de desempenho | ✅ | `docs/performance.md` |
| Pipeline CI | ✅ | `.github/workflows/ci.yml` (backend + gitleaks + sonar opcional) |
| JWT | ⏳ Dia 2 | `TODO(Dia 2)` em `SecurityConfig` |
| Importador Gemini | ⏳ Dia 2 | — |

## Decisões tomadas durante a execução

### 1. Multi-select parcial = qualidade 3, não lapso

Os testes falharam primeiro: `ReviewQuality.evaluate` retornava `INCORRECT` para quem
acertava *parte* de uma questão "Choose TWO". Era ambíguo na SPEC (seção 4).

**Decidido:** acertar parcialmente é progresso, não lapso — zerar a sequência de quem
está quase lá pune o comportamento correto. Fica como `CORRECT_WITH_DIFFICULTY` (q=3),
preservando a repetição e reduzindo o crescimento do intervalo.

### 2. O intervalo usa o EF **já atualizado**

Teste `weakCorrectDoesNotReset` esperava 15 dias; o código entregou 14. O código estava
certo (round(6 × 2,36) = 14) — o teste tinha assumido o EF antigo. Corrigido o teste,
não o código. Registrando aqui porque inverter isso silenciosamente seria o tipo de
"ajuste de teste para passar" que destrói a confiança na suíte.

### 3. Dois scripts de teste de integração, um contrato

`ReviewFlowIT` (Testcontainers, roda no CI) e `ReviewFlowLocalIT` (banco externo via
`TEST_DB_URL`, roda sem Docker). Ambos herdam de `ReviewFlowContract` — mesma bateria,
duas infraestruturas. Sem isso, o desenvolvedor sem Docker não roda integração nenhuma.

## Bugs encontrados pelo próprio pipeline (e corrigidos)

| Sintoma | Causa raiz | Correção |
|---|---|---|
| `*IT` nunca executava | Surefire só roda `*Test` | adicionado `maven-failsafe-plugin` |
| 4 testes de integração em 415 | `TestRestTemplate` envia `String` como `text/plain` | helper `postJson()` com `Content-Type: application/json` |
| Cobertura 50% "inexplicável" | integração não rodava (acima) | rodando ITs, cobertura real = 91,3% |

> Estes três são o argumento inteiro a favor de portões de qualidade: o build estava
> "verde" e a suíte de integração simplesmente não existia na prática.

## Números reais (medidos, não estimados)

```
Testes unitários ......... 36  (todos verdes)
Testes de integração ..... 5   (todos verdes, Postgres real)
Cobertura de linhas ...... 91,3%  (264/289)
  domain.scheduler ....... 100%
  domain.model ........... 92,6%
  application ............ 100%
  api.controller ......... 100%
  persistence.adapter .... 100%
  api.error .............. 44,8%  ← único ponto abaixo do ideal
GET /reviews/due p95 ..... 25,5 ms
```

## Dívidas assumidas (conscientes, rastreadas)

1. **Sem autenticação até o Dia 2** — `SecurityConfig` libera tudo, com `TODO(Dia 2)`.
2. **`api.error` com 44,8% de cobertura** — os handlers de 403/500 não têm teste
   dedicado; o teste de 400 existe. Fechar no Dia 3.
3. **Sem paginação por cursor** — a fila do dia usa `limit` (máx. 100). Suficiente para
   o caso de uso, insuficiente se a base chegar a milhares de questões vencidas.
4. **Seed de demonstração no mesmo path de migration** — em produção, `V2__seed.sql`
   deveria sair (ou ser condicional por perfil).

## Próximo (Dia 2)

1. `LlmPort` + `GeminiClient` + validação de schema da resposta.
2. `POST /api/imports/parse` e `/confirm`, com WireMock nos testes (sem gastar cota).
3. JWT: login, filtro, `JwtCurrentUserProvider`, troca do `permitAll`.
4. Angular + Clarity: telas de Login e Importar.
