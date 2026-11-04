package com.spacedrep.api.mapper;

import com.spacedrep.api.dto.AnswerResponse;
import com.spacedrep.api.dto.CardResponse;
import com.spacedrep.api.dto.DueQueueResponse;
import com.spacedrep.api.dto.OptionResponse;
import com.spacedrep.application.view.AnswerResult;
import com.spacedrep.application.view.DueCard;
import com.spacedrep.application.view.DueQueue;
import com.spacedrep.domain.model.AnswerOption;
import com.spacedrep.domain.model.Question;

import java.util.List;

/** Traducao dominio -> DTO. Mantida fora do controller para nao misturar camadas. */
public final class ReviewMapper {

    private ReviewMapper() {
    }

    public static DueQueueResponse toResponse(DueQueue queue) {
        List<CardResponse> cards = queue.cards().stream()
                .map(ReviewMapper::toResponse)
                .toList();
        return new DueQueueResponse(cards, queue.remaining());
    }

    private static CardResponse toResponse(DueCard dueCard) {
        Question question = dueCard.question();
        List<OptionResponse> options = question.options().stream()
                .map(ReviewMapper::toResponse)
                .toList();
        return new CardResponse(
                dueCard.card().id(),
                question.id(),
                question.topic(),
                question.stem(),
                question.multiSelect(),
                options,
                dueCard.card().state().repetitions(),
                dueCard.card().state().intervalDays(),
                dueCard.card().dueDate());
    }

    private static OptionResponse toResponse(AnswerOption option) {
        return new OptionResponse(option.id(), option.position(), option.text());
    }

    public static AnswerResponse toResponse(AnswerResult result) {
        return new AnswerResponse(
                result.correct(),
                result.correctOptionIds(),
                result.explanation(),
                result.schedule().dueDate(),
                result.schedule().intervalDays(),
                result.schedule().easeFactor(),
                result.schedule().repetitions());
    }
}
