package com.spacedrep.infrastructure.persistence.repository;

import com.spacedrep.infrastructure.persistence.entity.QuestionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionJpaRepository extends JpaRepository<QuestionEntity, Long> {

    /** EntityGraph evita N+1 ao carregar as alternativas da questao. */
    @EntityGraph(attributePaths = "options")
    Optional<QuestionEntity> findWithOptionsById(Long id);
}
