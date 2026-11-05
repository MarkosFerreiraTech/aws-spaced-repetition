package com.spacedrep.application.port;

import java.time.Instant;

/** Porta de emissao de token. A implementacao (JWT) fica na infraestrutura. */
public interface TokenIssuer {

    record IssuedToken(String token, Instant expiresAt) {
    }

    IssuedToken issue(long userId, String email);
}
