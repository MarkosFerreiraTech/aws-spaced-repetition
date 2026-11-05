package com.spacedrep.application.port;

import com.spacedrep.domain.model.DraftQuestion;

import java.util.List;

/**
 * Porta do provedor de LLM.
 *
 * <p>O dominio e a aplicacao nao sabem que existe Gemini. Trocar de provedor
 * (OpenAI, modelo local) e criar outra implementacao desta interface.</p>
 */
public interface LlmPort {

    /**
     * @throws com.spacedrep.domain.exception.InvalidLlmResponseException
     *         quando a resposta nao pode ser estruturada em questoes
     * @throws com.spacedrep.domain.exception.LlmUnavailableException
     *         quando o provedor falha apos as tentativas
     */
    List<DraftQuestion> parseQuestions(String examName, String rawText);
}
