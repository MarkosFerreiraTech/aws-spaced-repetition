package com.spacedrep.infrastructure.config;

import com.spacedrep.domain.scheduler.Scheduler;
import com.spacedrep.domain.scheduler.Sm2Scheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Liga o dominio puro ao container do Spring.
 * O dominio nao conhece o framework; quem conhece e a infraestrutura.
 */
@Configuration
public class DomainConfig {

    /** Trocar SM-2 por FSRS (roadmap M2) e trocar esta linha. */
    @Bean
    public Scheduler scheduler() {
        return new Sm2Scheduler();
    }

    /**
     * Relogio injetavel: torna "hoje" testavel e impede o fuso do servidor
     * (UTC) de decidir quando o dia do usuario vira.
     */
    @Bean
    public Clock clock(@Value("${app.zone-id:America/Sao_Paulo}") String zoneId) {
        return Clock.system(ZoneId.of(zoneId));
    }
}
