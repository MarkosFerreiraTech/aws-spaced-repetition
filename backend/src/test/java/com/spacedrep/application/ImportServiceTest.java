package com.spacedrep.application;

import com.spacedrep.application.importing.ImportRateLimiter;
import com.spacedrep.application.importing.ImportService;
import com.spacedrep.application.importing.RawTextHasher;
import com.spacedrep.application.port.LlmPort;
import com.spacedrep.domain.exception.DuplicateImportException;
import com.spacedrep.domain.exception.ImportTooLargeException;
import com.spacedrep.domain.exception.InvalidImportException;
import com.spacedrep.domain.exception.InvalidLlmResponseException;
import com.spacedrep.domain.exception.RateLimitExceededException;
import com.spacedrep.domain.model.DraftOption;
import com.spacedrep.domain.model.DraftQuestion;
import com.spacedrep.domain.port.ExamImportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImportService (importador em duas etapas)")
class ImportServiceTest {

    private static final long USER_ID = 1L;
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private LlmPort llm;

    @Mock
    private ExamImportRepository importRepository;

    private ImportService service;

    @BeforeEach
    void setUp() {
        service = new ImportService(llm, importRepository,
                new ImportRateLimiter(100, CLOCK), CLOCK, 1_000);
    }

    private static DraftQuestion draft(String stem) {
        return new DraftQuestion(stem, "IAM", false, "exp",
                List.of(new DraftOption(1, "correta", true), new DraftOption(2, "errada", false)));
    }

    @Test
    @DisplayName("parse devolve rascunhos e o hash do texto, sem gravar nada")
    void parseReturnsDraftsAndHash() {
        when(llm.parseQuestions(anyString(), anyString())).thenReturn(List.of(draft("Q1"), draft("Q2")));

        ImportService.ParseResult result = service.parse(USER_ID, "Simulado", "texto bruto");

        assertThat(result.questions()).hasSize(2);
        assertThat(result.rawTextHash()).isEqualTo(RawTextHasher.sha256Hex("texto bruto"));
        verify(importRepository, never()).save(any(), anyList(), any());
    }

    @Test
    @DisplayName("texto vazio e rejeitado antes de chamar o LLM (nao gasta cota)")
    void blankTextIsRejectedBeforeCallingLlm() {
        assertThatThrownBy(() -> service.parse(USER_ID, "Simulado", "   "))
                .isInstanceOf(InvalidImportException.class);

        verify(llm, never()).parseQuestions(anyString(), anyString());
    }

    @Test
    @DisplayName("texto acima do limite retorna erro de payload")
    void oversizedTextIsRejected() {
        String tooLong = "x".repeat(1_001);

        assertThatThrownBy(() -> service.parse(USER_ID, "Simulado", tooLong))
                .isInstanceOf(ImportTooLargeException.class);
        verify(llm, never()).parseQuestions(anyString(), anyString());
    }

    @Test
    @DisplayName("LLM sem questoes retorna 422")
    void emptyLlmResultIsUnprocessable() {
        when(llm.parseQuestions(anyString(), anyString())).thenReturn(List.of());

        assertThatThrownBy(() -> service.parse(USER_ID, "Simulado", "texto"))
                .isInstanceOf(InvalidLlmResponseException.class);
    }

    @Test
    @DisplayName("limite de importacoes por minuto e respeitado")
    void rateLimitIsEnforced() {
        ImportService limited = new ImportService(llm, importRepository,
                new ImportRateLimiter(1, CLOCK), CLOCK, 1_000);
        when(llm.parseQuestions(anyString(), anyString())).thenReturn(List.of(draft("Q1")));

        limited.parse(USER_ID, "Simulado", "texto");
        assertThatThrownBy(() -> limited.parse(USER_ID, "Simulado", "texto"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("confirm grava origem, questoes e cards vencendo hoje")
    void confirmPersistsExam() {
        when(importRepository.existsByRawTextHash(anyString())).thenReturn(false);
        when(importRepository.save(any(), anyList(), any())).thenReturn(42L);

        ImportService.ImportedExam imported = service.confirm(
                USER_ID, "Simulado", "hash-fake", List.of(draft("Q1"), draft("Q2")));

        assertThat(imported.examSourceId()).isEqualTo(42L);
        assertThat(imported.createdQuestions()).isEqualTo(2);

        ArgumentCaptor<LocalDate> dueDate = ArgumentCaptor.forClass(LocalDate.class);
        verify(importRepository).save(any(), anyList(), dueDate.capture());
        assertThat(dueDate.getValue()).isEqualTo(LocalDate.of(2026, 11, 4));
    }

    @Test
    @DisplayName("reimportar o mesmo texto e bloqueado antes de gravar")
    void duplicateImportIsRejectedBeforeSaving() {
        when(importRepository.existsByRawTextHash("hash-fake")).thenReturn(true);

        assertThatThrownBy(() -> service.confirm(USER_ID, "Simulado", "hash-fake", List.of(draft("Q1"))))
                .isInstanceOf(DuplicateImportException.class);

        verify(importRepository, never()).save(any(), anyList(), any());
    }

    @Test
    @DisplayName("confirm sem questoes e rejeitado")
    void confirmWithoutQuestionsIsRejected() {
        assertThatThrownBy(() -> service.confirm(USER_ID, "Simulado", "hash-fake", List.of()))
                .isInstanceOf(InvalidImportException.class);

        verify(importRepository, never()).save(any(), anyList(), any());
    }
}
