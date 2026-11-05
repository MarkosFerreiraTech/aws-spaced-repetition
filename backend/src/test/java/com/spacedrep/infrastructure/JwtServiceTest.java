package com.spacedrep.infrastructure;

import com.nimbusds.jwt.JWTClaimsSet;
import com.spacedrep.infrastructure.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JWT (emissao e validacao)")
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-with-at-least-32-bytes";
    private static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final JwtService service = new JwtService(SECRET, 60, CLOCK);

    @Test
    @DisplayName("token emitido carrega o id do usuario e o e-mail")
    void issuedTokenCarriesSubject() throws Exception {
        var issued = service.issue(7L, "markos@exemplo.com");

        JWTClaimsSet claims = service.verify(issued.token());

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.getStringClaim("email")).isEqualTo("markos@exemplo.com");
        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(60)));
    }

    @Test
    @DisplayName("token adulterado e rejeitado")
    void tamperedTokenIsRejected() {
        String token = service.issue(7L, "markos@exemplo.com").token();
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> service.verify(tampered))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("token expirado e rejeitado")
    void expiredTokenIsRejected() {
        String token = service.issue(7L, "markos@exemplo.com").token();

        JwtService laterService = new JwtService(SECRET, 60,
                Clock.fixed(NOW.plus(Duration.ofHours(2)), ZoneOffset.UTC));

        assertThatThrownBy(() -> laterService.verify(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    @DisplayName("segredo com menos de 32 bytes impede o start da aplicacao")
    void shortSecretFailsFast() {
        assertThatThrownBy(() -> new JwtService("curto", 60, CLOCK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("token assinado com outro segredo e rejeitado")
    void tokenFromAnotherSecretIsRejected() {
        JwtService other = new JwtService("outro-segredo-com-32-bytes-ou-mais-aqui", 60, CLOCK);
        String foreignToken = other.issue(7L, "markos@exemplo.com").token();

        assertThatThrownBy(() -> service.verify(foreignToken))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
