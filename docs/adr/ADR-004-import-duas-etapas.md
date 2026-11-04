# ADR-004 — Import em duas etapas (parse → revisão → confirm)

**Status:** aceito · **Data:** 2026-11-04

## Contexto
O caminho mais curto seria `POST /imports` → Gemini → `save()`. Mas a saída de um LLM é **probabilística**: ele funde questões, perde a alternativa correta de questões multi-resposta e, pior, **inventa explicações plausíveis**. Uma base de estudo contaminada por explicação alucinada não gera erro visível — gera aprendizado errado. É o pior tipo de falha: silenciosa.

## Decisão
Separar em dois endpoints:
1. `POST /api/imports/parse` → chama o LLM, valida o JSON contra schema, devolve rascunho, **não persiste**.
2. Front exibe o rascunho em grid editável (usuário corrige).
3. `POST /api/imports/confirm` → valida de novo e persiste.

## Consequências
- ✅ Erro do LLM passa a ser **falha de UI visível** em vez de dado corrompido.
- ✅ Elimina a necessidade de job assíncrono: o parse roda síncrono (com timeout de 45s) porque nada é escrito antes da confirmação.
- ✅ Habilita dedup simples por hash do texto bruto (409 em reimportação).
- ⚠️ Uma tela a mais e um passo manual. Aceito de bom grado: é o que separa "protótipo que impressiona" de "ferramenta que se usa por semanas".
