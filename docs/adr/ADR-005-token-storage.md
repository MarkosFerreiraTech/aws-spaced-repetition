# ADR-005 — Token JWT em memória/`sessionStorage` em vez de cookie HttpOnly

**Status:** aceito (com dívida assumida) · **Data:** 2026-11-04

## Contexto
Onde guardar o JWT é uma decisão de segurança com trade-offs reais, não de gosto.

| Opção | Risco XSS | Risco CSRF | Esforço |
|---|---|---|---|
| `localStorage` | Alto (qualquer script lê) | Nenhum | Trivial |
| `sessionStorage`/memória | Alto ao ler, mas expira ao fechar a aba | Nenhum | Trivial |
| Cookie `HttpOnly` + `SameSite=Strict` | Baixo | Baixo com SameSite | **Exige CSRF token no backend + CORS com credenciais** |

## Decisão
M0: token em memória, com `sessionStorage` como persistência de aba. Cookie `HttpOnly` fica para o M1, quando houver BFF.

## Consequências
- ✅ Entrega no prazo; reduz a superfície em relação a `localStorage` (opção mais comum e pior).
- ⚠️ **Dívida de segurança declarada:** XSS no front compromete o token. Mitigação parcial no M0: Angular escapa interpolação por padrão, `innerHTML` proibido, CSP básica por header.
- ⚠️ Quando houver BFF (M2), o token deixa de existir no browser — a decisão se torna obsoleta por construção.
