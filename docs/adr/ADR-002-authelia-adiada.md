# ADR-002 — JWT no Spring Security; Authelia adiada para M2

**Status:** aceito · **Data:** 2026-11-04

## Contexto
A spec original pede **duas** camadas de autenticação: JWT por transação e Authelia.

Authelia não é uma biblioteca que se adiciona ao Spring: é um **portal SSO** que opera como forward-auth atrás de um reverse proxy (Traefik/Nginx/Caddy) e depende de um **backend de usuários** (arquivo, LLDAP ou LDAP). Colocá-la em pé exige: proxy configurado, domínio, TLS, base de usuários, políticas de 2FA e integração do token com o Spring como resource server. Isso é 1 dia inteiro de infraestrutura — metade do prazo, zero de produto.

## Decisão
No M0, o **Spring Security é o emissor e validador** do JWT (HS256, expiração 8h, sem refresh token). Todas as rotas `/api/**` (exceto login) exigem Bearer token. Authelia entra no **M2**, atrás de reverse proxy, protegendo o portal — o backend já estará pronto para receber tokens externos via `JwtDecoder`.

## Consequências
- ✅ Requisito "JWT em toda transação" atendido integralmente e testado.
- ✅ `SecurityConfig` já usa `SecurityFilterChain` + resource server, então a migração para Authelia é troca de `JwtDecoder`, não reescrita.
- ⚠️ Sem 2FA e sem SSO no M0.
- ⚠️ **Requisito reformulado:** "somente o frontend apto a chamar a API" não é garantível em SPA — o cliente pode extrair o próprio token do DevTools e CORS não é controle de acesso. O requisito efetivo passa a ser "somente usuário autenticado". Para esconder o token do browser de verdade, o padrão seria BFF (M2, opcional).
