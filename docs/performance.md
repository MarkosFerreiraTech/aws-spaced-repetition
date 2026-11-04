# Baseline de desempenho — Dia 1

Medição real, não estimativa. Ambiente: sandbox Linux, 2 vCPU, 1 GB RAM,
JVM 21 (Temurin), Spring Boot 3.3.5, PostgreSQL 17 local, 3 questões no banco.

## Método

50 requisições sequenciais por endpoint, `curl -w '%{time_total}'`, sem aquecimento
descartado (os números incluem a primeira chamada). Medir p95, não só a média:
a média esconde a cauda, e é a cauda que o usuário sente.

## Resultados

| Endpoint | Média | p95 | Máx | Meta | Status |
|---|---|---|---|---|---|
| `GET /api/reviews/due?limit=20` | 20,5 ms | **25,5 ms** | 30,7 ms | < 200 ms | ✅ |
| `GET /actuator/health` | 6,1 ms | 8,3 ms | 9,7 ms | < 50 ms | ✅ |

## Limitações desta medição (honestidade metodológica)

- Base de dados com 3 questões: o índice `idx_review_card_due (user_id, due_date)` não
  é exercitado sob carga. Com 5.000 questões o número muda.
- Execução sequencial: não mede contenção de pool de conexões nem concorrência.
- Sem carga concorrente (k6 fica no M1).

## Próximos passos (M1)

- `k6` com 50 VUs por 2 min nos endpoints de leitura.
- Massa de teste com 5.000 questões / 20.000 tentativas.
- Medir separadamente o `POST /imports/parse`: a latência é dominada pelo LLM
  (segundos), não pelo backend — misturar as duas métricas esconde regressão real.
