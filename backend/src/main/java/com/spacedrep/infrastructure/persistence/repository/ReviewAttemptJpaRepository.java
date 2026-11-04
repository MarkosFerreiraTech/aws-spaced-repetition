package com.spacedrep.infrastructure.persistence.repository;

import com.spacedrep.infrastructure.persistence.entity.ReviewAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewAttemptJpaRepository extends JpaRepository<ReviewAttemptEntity, Long> {
}
