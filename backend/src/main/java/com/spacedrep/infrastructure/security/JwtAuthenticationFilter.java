package com.spacedrep.infrastructure.security;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Extrai e valida o Bearer token em cada requisicao. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            try {
                JWTClaimsSet claims = jwtService.verify(header.substring(PREFIX.length()));
                long userId = Long.parseLong(claims.getSubject());
                String email = claims.getStringClaim("email");

                var authentication = new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(userId, email), null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ex) {
                // token invalido nao derruba a requisicao aqui: ela segue sem autenticacao
                // e o SecurityFilterChain responde 401. Log sem o token (dado sensivel).
                SecurityContextHolder.clearContext();
                logger.debug("Bearer token rejeitado: " + ex.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
