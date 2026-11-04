package com.spacedrep.domain.port;

import com.spacedrep.domain.model.ReviewCard;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Porta de persistencia de cards (implementada em infrastructure.persistence). */
public interface ReviewCardRepository {

    List<ReviewCard> findDue(long userId, LocalDate date, int limit);

    long countDue(long userId, LocalDate date);

    Optional<ReviewCard> findById(long cardId);

    ReviewCard save(ReviewCard card);
}
