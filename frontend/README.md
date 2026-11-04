# Frontend (Dia 2)

Angular + VMware Clarity (design system compliance).

## Escopo previsto

| Tela | Componentes Clarity |
|---|---|
| Login | `clr-input-container`, `clr-password-container` |
| Importar simulado | `clr-textarea-container`, `clr-datagrid` (revisão editável) |
| Estudar | `clr-radio-wrapper`, `clr-checkbox-wrapper`, `clr-progress-bar` |
| Painel | `clr-cards`, `clr-datagrid` |

## Antes de começar (risco R1 da SPEC)

A versão do `@clr/angular` precisa ser compatível com a versão do Angular instalada.
Verificar **na primeira hora**:

```bash
ng new frontend --routing --style=scss
cd frontend && ng add @clr/angular
```

Se houver incompatibilidade, fixar o Angular na versão suportada pelo Clarity
(`npm view @clr/angular peerDependencies`).

## Contrato consumido

- `GET /api/reviews/due?limit=20`
- `POST /api/reviews/{cardId}/answer`

Interceptors previstos: `AuthInterceptor` (Bearer token, Dia 2), `ErrorInterceptor`
(leitura de `application/problem+json`).
