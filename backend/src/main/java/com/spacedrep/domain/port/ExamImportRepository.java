package com.spacedrep.domain.port;

import com.spacedrep.domain.model.ExamSource;
import com.spacedrep.domain.model.Question;

import java.time.LocalDate;
import java.util.List;

/**
 * Porta de escrita do import.
 *
 * <p>Recebe o agregado inteiro (origem + questoes + cards) porque a persistencia de
 * um import e atomica: ou tudo entra, ou nada entra.</p>
 */
public interface ExamImportRepository {

    boolean existsByRawTextHash(String rawTextHash);

    /**
     * Persiste origem + questoes + alternativas e cria um card vencendo em
     * {@code initialDueDate} para cada questao. O vinculo card -> questao depende
     * do id gerado no insert, por isso e feito aqui e nao na aplicacao.
     *
     * @return id da origem criada
     */
    long save(ExamSource source, List<Question> questions, LocalDate initialDueDate);
}
