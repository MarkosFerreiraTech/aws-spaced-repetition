package com.spacedrep.application;

/**
 * Porta para descobrir o usuario da requisicao.
 *
 * <p>Dia 1: implementacao de demonstracao (usuario fixo do seed).
 * Dia 2: implementacao que le o subject do JWT — troca de bean, nao de servico.</p>
 */
public interface CurrentUserProvider {

    long currentUserId();
}
