package com.spacedrep.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracao de seguranca do DIA 1.
 *
 * <p><b>DIVIDA CONSCIENTE (fechada no Dia 2):</b> todos os endpoints estao liberados,
 * porque JWT ainda nao foi implementado. O Dia 2 troca {@code permitAll()} por
 * {@code .requestMatchers("/api/auth/login").permitAll().anyRequest().authenticated()}
 * e adiciona o filtro de JWT + {@code JwtCurrentUserProvider} (ADR-002).</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // TODO(Dia 2): .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                //              .requestMatchers("/actuator/health").permitAll()
                //              .anyRequest().authenticated()
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
