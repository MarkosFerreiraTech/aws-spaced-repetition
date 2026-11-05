package com.spacedrep.infrastructure.llm;

import com.spacedrep.domain.model.DraftQuestion;

import java.util.List;

/** Envelope do JSON devolvido pelo LLM. */
public record ParsedQuestions(List<DraftQuestion> questions) {
}
