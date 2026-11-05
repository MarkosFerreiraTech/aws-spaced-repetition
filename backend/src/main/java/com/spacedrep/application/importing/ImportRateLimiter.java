package com.spacedrep.application.importing;

import com.spacedrep.domain.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de importacoes por usuario por minuto.
 *
 * <p>Protege duas coisas: a cota da API do Gemini (custo) e o proprio servidor
 * (cada parse e uma chamada lenta a um terceiro).</p>
 *
 * <p>Implementacao em memoria: suficiente para instanciaunica. Com mais de uma
 * instancia, isso vira Redis (M1).</p>
 */
@Component
public class ImportRateLimiter {

    private final int limitPerMinute;
    private final Clock clock;
    private final Map<Long, Deque<Instant>> hits = new ConcurrentHashMap<>();

    public ImportRateLimiter(@Value("${app.import.rate-limit-per-minute:5}") int limitPerMinute,
                             Clock clock) {
        this.limitPerMinute = limitPerMinute;
        this.clock = clock;
    }

    public void checkLimit(long userId) {
        Instant now = clock.instant();
        Instant windowStart = now.minus(Duration.ofMinutes(1));
        Deque<Instant> userHits = hits.computeIfAbsent(userId, key -> new ArrayDeque<>());
        synchronized (userHits) {
            while (!userHits.isEmpty() && userHits.peekFirst().isBefore(windowStart)) {
                userHits.pollFirst();
            }
            if (userHits.size() >= limitPerMinute) {
                throw new RateLimitExceededException(
                        "Limite de " + limitPerMinute + " importacoes por minuto excedido.");
            }
            userHits.addLast(now);
        }
    }
}
