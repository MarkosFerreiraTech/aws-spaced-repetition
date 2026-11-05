# Relatório — Dia 2 (fechamento)

**Data:** 2026-11-05 · **Escopo:** importador inteligente + autenticação JWT (SPEC §2, §5, §8)

## Entregue

| Item | Status | Evidência |
|---|---|---|
| Importador em duas etapas | ✅ | `POST /api/imports/parse` (não grava) + `/confirm` (grava) |
| Integração com Gemini | ✅ | `GeminiClient` atrás da porta `LlmPort` |
| Validação de saída do LLM | ✅ | `DraftQuestion` + schema; erro vira 422, não 500 |
| Deduplicação de import | ✅ | SHA-256 do texto bruto → 409 |
| Rate limit por usuário | ✅ | 5/min, 429 |
| JWT HS256 | ✅ | login, filtro, `JwtCurrentUserProvider`, 401 em RFC 9457 |
| Authelia | ⏳ M2 | ADR-002 mantido |
| Frontend Angular + Clarity | ⏳ Dia 3 | — |

## Números

```
Testes unitários ......... 63
Testes de integração ..... 10  (Postgres real + Gemini falso via WireMock)
Cobertura de linhas ...... 90,4%
  application.importing .. 94,4%
  infrastructure.llm ..... 87,0%
  infrastructure.security  93,9%
  api.controller ......... 100%
```

## Bugs reais encontrados pelos testes (não pelo usuário)

### 1. O domínio assumia que toda alternativa já tinha id
`Question.correctOptionIds()` usava `Collectors.toUnmodifiableSet()`, que estoura
`NullPointerException` quando algum id é `null`. Isso acontece sempre que a questão
é montada a partir de um rascunho — ou seja, **todo import quebraria em produção**.
A validação passou a contar alternativas corretas diretamente, e a coleção de ids
ignora `null`. O domínio deixou de depender de identidade de persistência.

### 2. `ddl-auto=validate` recusou `CHAR(64)` vs `VARCHAR`
A V1 declarava `raw_text_hash CHAR(64)`; o Postgres implementa `CHAR` como `bpchar`
e o Hibernate recusa. Corrigido com a migration **V3**, não editando a V1: migration
já aplicada tem checksum, e editar quebra todo ambiente existente.

### 3. `@ActiveProfiles` no teste abstrato não era herdado
O `application-test.yml` nunca carregava, a API key do Gemini ficava vazia e o import
respondia 503 em cascata. Trocado por `@TestPropertySource`, que é herdado de fato.

### 4. 401 com corpo + cliente HTTP do JDK
`HttpRetryException: cannot retry due to server authentication, in streaming mode`.
Testes de login inválido usam `HttpComponentsClientHttpRequestFactory` em vez do
cliente JDK.

## Decisões de segurança registradas

| Decisão | Motivo |
|---|---|
| API key do Gemini em **header**, nunca na URL | URL vaza em log de proxy e stacktrace |
| Texto colado delimitado no prompt + instrução fixa | o texto é input não confiável (prompt injection) |
| Mensagem de erro idêntica para usuário inexistente e senha errada | evita enumeração de usuários |
| Token nunca logado | `JwtAuthenticationFilter` loga só o motivo da rejeição |
| `app.jwt.secret` mínimo de 32 bytes com falha no start | HS256 exige 256 bits; melhor não subir do que subir inseguro |
| Rate limit no `/parse` | protege a cota do Gemini e o próprio servidor |

## Dívidas assumidas

1. `api.error` com 34,5% de cobertura — os handlers de 403/409/429/503 não têm teste
   dedicado de contrato HTTP (o comportamento está coberto via serviço).
2. Rate limit em memória: com mais de uma instância, vira Redis.
3. Sem refresh token: expira em 8h e o usuário refaz login.
4. `/imports/parse` é síncrono (timeout de 45s). Para simulados muito grandes, o
   caminho é job assíncrono com polling (M1).

## Próximo (Dia 3)

1. Frontend Angular + Clarity: Login, Importar (parse → grid de revisão → confirmar),
   Estudar, Painel.
2. Branch protection + SonarCloud com as chaves do projeto.
3. `docs/performance.md` atualizado com os endpoints novos.
4. Demo de 3 minutos e tag `v0.2.0`.
