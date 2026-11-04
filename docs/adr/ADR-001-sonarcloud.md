# ADR-001 — SonarCloud em vez de SonarQube self-hosted

**Status:** aceito · **Data:** 2026-11-04

## Contexto
O requisito é "portões de qualidade com SonarQube". SonarQube Server exige container próprio, banco dedicado, provisionamento de token e administração — infraestrutura que não produz nenhuma linha de produto.

## Decisão
Usar **SonarCloud** (mesmo engine, mesma engine de regras e mesmo conceito de Quality Gate) em repositório público, acionado pelo Maven no GitHub Actions.

## Consequências
- ✅ Quality gate real e bloqueante desde o dia 2, sem custo nem infra.
- ✅ As regras (`new_coverage`, bugs, vulnerabilidades, duplicação) são as mesmas que se usaria localmente.
- ⚠️ Exige repositório público. Se o projeto precisar ser privado, o M1 migra para SonarQube em container (o workflow muda em ~10 linhas).
- ⚠️ Análise roda no CI, não localmente por padrão (é possível rodar `mvn sonar:sonar` local com token).
