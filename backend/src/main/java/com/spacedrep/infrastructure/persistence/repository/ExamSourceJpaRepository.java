package com.spacedrep.infrastructure.persistence.repository;

import com.spacedrep.infrastructure.persistence.entity.ExamSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamSourceJpaRepository extends JpaRepository<ExamSourceEntity, Long> {

    boolean existsByRawTextHash(String rawTextHash);
}
