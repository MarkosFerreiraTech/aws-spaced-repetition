package com.spacedrep.application.importing;

import com.spacedrep.application.port.LlmPort;
import com.spacedrep.domain.exception.DuplicateImportException;
import com.spacedrep.domain.exception.ImportTooLargeException;
import com.spacedrep.domain.exception.InvalidImportException;
import com.spacedrep.domain.exception.InvalidLlmResponseException;
import com.spacedrep.domain.model.AnswerOption;
import com.spacedrep.domain.model.DraftQuestion;
import com.spacedrep.domain.model.ExamSource;
import com.spacedrep.domain.model.Question;
import com.spacedrep.domain.port.ExamImportRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Caso de uso do importador inteligente (ADR-004: duas etapas).
 *
 * <ol>
 *   <li>{@link #parse} chama o LLM e devolve rascunhos — <b>nao persiste nada</b>;</li>
 *   <li>{@link #confirm} recebe os rascunhos revisados pelo usuario e grava.</li>
 * </ol>
 *
 * <p>A separacao existe porque a saida de um LLM e probabilistica: gravar direto
 * contamina a base de estudo de forma silenciosa.</p>
 */
@Service
public class ImportService {

    private final LlmPort llm;
    private final ExamImportRepository importRepository;
    private final ImportRateLimiter rateLimiter;
    private final Clock clock;
    private final int maxTextLength;

    public ImportService(LlmPort llm,
                         ExamImportRepository importRepository,
                         ImportRateLimiter rateLimiter,
                         Clock clock,
                         @Value("${app.import.max-text-length:100000}") int maxTextLength) {
        this.llm = llm;
        this.importRepository = importRepository;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
        this.maxTextLength = maxTextLength;
    }

    /** Etapa 1: texto bagunçado -> rascunhos estruturados (sem gravar). */
    public ParseResult parse(long userId, String examName, String rawText) {
        rateLimiter.checkLimit(userId);
        validateText(rawText);

        List<DraftQuestion> drafts = llm.parseQuestions(examName, rawText);
        if (drafts == null || drafts.isEmpty()) {
            throw new InvalidLlmResponseException(
                    "Nenhuma questao foi identificada no texto enviado.");
        }
        return new ParseResult(drafts, RawTextHasher.sha256Hex(rawText));
    }

    /** Etapa 2: rascunhos revisados -> banco. */
    @Transactional
    public ImportedExam confirm(long userId, String examName, String rawTextHash,
                                List<DraftQuestion> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            throw new InvalidImportException("Nenhuma questao para importar.");
        }
        if (importRepository.existsByRawTextHash(rawTextHash)) {
            throw new DuplicateImportException();
        }

        List<Question> questions = drafts.stream().map(ImportService::toQuestion).toList();
        ExamSource source = ExamSource.newSource(userId, examName, rawTextHash);
        long examSourceId = importRepository.save(source, questions, LocalDate.now(clock));

        return new ImportedExam(examSourceId, questions.size());
    }

    private void validateText(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new InvalidImportException("O texto do simulado esta vazio.");
        }
        if (rawText.length() > maxTextLength) {
            throw new ImportTooLargeException(
                    "Texto com " + rawText.length() + " caracteres excede o limite de "
                            + maxTextLength + ".");
        }
    }

    private static Question toQuestion(DraftQuestion draft) {
        List<AnswerOption> options = draft.options().stream()
                .map(option -> new AnswerOption(null, option.position(), option.text(), option.correct()))
                .toList();
        return Question.rehydrate(null, draft.topic(), draft.stem(), draft.explanation(),
                draft.multiSelect(), options);
    }

    /** Rascunhos + hash do texto (o cliente devolve o hash no confirm). */
    public record ParseResult(List<DraftQuestion> questions, String rawTextHash) {
    }

    /** Resultado do import persistido. */
    public record ImportedExam(long examSourceId, int createdQuestions) {
    }
}
