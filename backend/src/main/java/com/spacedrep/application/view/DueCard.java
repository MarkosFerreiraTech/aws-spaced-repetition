package com.spacedrep.application.view;

import com.spacedrep.domain.model.Question;
import com.spacedrep.domain.model.ReviewCard;

/** Card vencido junto com a questao que ele representa. */
public record DueCard(ReviewCard card, Question question) {
}
