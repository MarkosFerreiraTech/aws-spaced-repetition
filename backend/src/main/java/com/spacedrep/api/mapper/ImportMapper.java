package com.spacedrep.api.mapper;

import com.spacedrep.api.dto.ConfirmImportResponse;
import com.spacedrep.api.dto.DraftOptionDto;
import com.spacedrep.api.dto.DraftQuestionDto;
import com.spacedrep.api.dto.ParseImportResponse;
import com.spacedrep.application.importing.ImportService;
import com.spacedrep.domain.exception.InvalidImportException;
import com.spacedrep.domain.model.DraftOption;
import com.spacedrep.domain.model.DraftQuestion;

import java.util.List;

public final class ImportMapper {

    private ImportMapper() {
    }

    public static ParseImportResponse toResponse(ImportService.ParseResult result) {
        return new ParseImportResponse(
                result.questions().stream().map(ImportMapper::toDto).toList(),
                result.rawTextHash(),
                result.questions().size());
    }

    public static ConfirmImportResponse toResponse(ImportService.ImportedExam imported) {
        return new ConfirmImportResponse(imported.examSourceId(), imported.createdQuestions());
    }

    public static DraftQuestion toDomain(DraftQuestionDto dto) {
        try {
            List<DraftOption> options = dto.options() == null ? List.of()
                    : dto.options().stream()
                            .map(option -> new DraftOption(option.position(), option.text(), option.correct()))
                            .toList();
            return new DraftQuestion(dto.stem(), dto.topic(), dto.multiSelect(),
                    dto.explanation(), options);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidImportException(
                    "Questao invalida no envio: " + ex.getMessage());
        }
    }

    private static DraftQuestionDto toDto(DraftQuestion draft) {
        return new DraftQuestionDto(
                draft.stem(), draft.topic(), draft.multiSelect(), draft.explanation(),
                draft.options().stream()
                        .map(option -> new DraftOptionDto(option.position(), option.text(), option.correct()))
                        .toList());
    }
}
