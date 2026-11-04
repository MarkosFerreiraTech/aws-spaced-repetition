package com.spacedrep.domain.port;

import com.spacedrep.domain.model.Question;

import java.util.Optional;

public interface QuestionRepository {

    Optional<Question> findById(long questionId);
}
