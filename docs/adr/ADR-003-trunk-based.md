# ADR-003 — Trunk-based simplificado em vez de Gitflow completo

**Status:** aceito · **Data:** 2026-11-04

## Contexto
O requisito pede Gitflow (branches `develop`, `release/*`, `hotfix/*`). Gitflow foi desenhado para software com versões mantidas em paralelo por múltiplas equipes; o próprio autor (Vincent Driessen) registrou anos depois que, para produtos com entrega contínua, fluxos mais simples funcionam melhor. Aqui: um desenvolvedor, 3 dias, deploy contínuo na `main`.

## Decisão
Trunk-based simplificado:
- `main` protegida (PR obrigatório + checks requeridos + squash merge).
- Branches curtas `feat/*`, `fix/*`, `docs/*`, vida útil ≤ 1 dia.
- **Conventional Commits** obrigatórios, commits atômicos.
- **SemVer** por tag manual (`v0.1.0-alpha`, `v0.1.0-beta`, `v0.1.0`).

## Consequências
- ✅ Mantém o que a formação realmente avalia: histórico legível, commits atômicos, semver, PR revisado, CI bloqueante.
- ✅ Elimina merges cerimoniais e conflitos de `develop` que consumiriam horas de um prazo de 72h.
- ⚠️ Desvio consciente e documentado, não esquecimento. Se a avaliação exigir a topologia Gitflow, criar `develop` e `release/0.1.0` no último dia é trabalho de 15 minutos, sem impacto no código.
