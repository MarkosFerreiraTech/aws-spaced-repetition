package com.spacedrep.api.controller;

import com.spacedrep.api.dto.ConfirmImportRequest;
import com.spacedrep.api.dto.ConfirmImportResponse;
import com.spacedrep.api.dto.ParseImportRequest;
import com.spacedrep.api.dto.ParseImportResponse;
import com.spacedrep.api.mapper.ImportMapper;
import com.spacedrep.application.CurrentUserProvider;
import com.spacedrep.application.importing.ImportService;
import com.spacedrep.domain.model.DraftQuestion;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Importador Inteligente — fluxo de duas etapas (ADR-004).
 *
 * <p>{@code /parse} nao grava nada: devolve rascunho para revisao humana.
 * O {@code /confirm} recebe as questoes revisadas e so entao persiste.</p>
 */
@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final ImportService importService;
    private final CurrentUserProvider currentUser;

    public ImportController(ImportService importService, CurrentUserProvider currentUser) {
        this.importService = importService;
        this.currentUser = currentUser;
    }

    @PostMapping("/parse")
    public ParseImportResponse parse(@Valid @RequestBody ParseImportRequest request) {
        return ImportMapper.toResponse(importService.parse(
                currentUser.currentUserId(), request.sourceName().trim(), request.rawText()));
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public ConfirmImportResponse confirm(@Valid @RequestBody ConfirmImportRequest request) {
        List<DraftQuestion> questions = request.questions().stream()
                .map(ImportMapper::toDomain)
                .toList();

        return ImportMapper.toResponse(importService.confirm(
                currentUser.currentUserId(), request.sourceName().trim(),
                request.rawTextHash(), questions));
    }
}
