package com.spacedrep.infrastructure.security;

import com.spacedrep.application.CurrentUserProvider;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Usuario da requisicao sempre vem do token verificado — nunca de um parametro do cliente. */
@Component
public class JwtCurrentUserProvider implements CurrentUserProvider {

    @Override
    public long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.userId();
        }
        throw new AuthenticationCredentialsNotFoundException("requisicao sem usuario autenticado");
    }
}
