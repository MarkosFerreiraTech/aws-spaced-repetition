package com.spacedrep.infrastructure.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.spacedrep.application.port.TokenIssuer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Emissao e validacao de JWT HS256 (ADR-002).
 *
 * <p>Usa Nimbus JOSE em vez de criptografia caseira: assinatura e verificacao
 * prontas, com validacao de expiracao.</p>
 */
@Component
public class JwtService implements TokenIssuer {

    private static final String ISSUER = "spaced-repetition-api";
    private static final int MIN_KEY_BYTES = 32;

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes:480}") long expirationMinutes,
                      Clock clock) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret precisa de ao menos " + MIN_KEY_BYTES + " bytes (HS256).");
        }
        this.key = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.expiration = Duration.ofMinutes(expirationMinutes);
        this.clock = clock;
    }

    @Override
    public IssuedToken issue(long userId, String email) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(expiration);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuer(ISSUER)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiresAt))
                .build();

        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(key));
            return new IssuedToken(jwt.serialize(), expiresAt);
        } catch (Exception ex) {
            throw new IllegalStateException("falha ao assinar o token", ex);
        }
    }

    /**
     * Verifica assinatura e expiracao.
     *
     * @throws IllegalArgumentException token invalido, adulterado ou expirado
     */
    public JWTClaimsSet verify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new MACVerifier(key))) {
                throw new IllegalArgumentException("assinatura do token invalida");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime == null || expirationTime.toInstant().isBefore(clock.instant())) {
                throw new IllegalArgumentException("token expirado");
            }
            if (!ISSUER.equals(claims.getIssuer())) {
                throw new IllegalArgumentException("emissor do token invalido");
            }
            return claims;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("token invalido", ex);
        }
    }
}
