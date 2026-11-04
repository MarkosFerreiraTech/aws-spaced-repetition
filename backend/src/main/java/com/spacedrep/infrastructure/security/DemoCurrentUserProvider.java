package com.spacedrep.infrastructure.security;

import com.spacedrep.application.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Implementacao do Dia 1: usuario fixo vindo da configuracao ($DEMO_USER_ID).
 *
 * <p>Dia 2 substitui por {@code JwtCurrentUserProvider}, que le o claim {@code sub}.
 * Ativa em todos os perfis exceto {@code jwt}.</p>
 */
@Component
@Profile("!jwt")
public class DemoCurrentUserProvider implements CurrentUserProvider {

    private final long demoUserId;

    public DemoCurrentUserProvider(@Value("${app.demo-user-id:1}") long demoUserId) {
        this.demoUserId = demoUserId;
    }

    @Override
    public long currentUserId() {
        return demoUserId;
    }
}
