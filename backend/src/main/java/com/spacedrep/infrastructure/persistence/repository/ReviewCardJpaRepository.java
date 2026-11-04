package com.spacedrep.infrastructure.persistence.repository;

import com.spacedrep.infrastructure.persistence.entity.ReviewCardEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReviewCardJpaRepository extends JpaRepository<ReviewCardEntity, Long> {

    @Query("""
            select c from ReviewCardEntity c
            where c.userId = :userId and c.dueDate <= :date
            order by c.dueDate asc, c.id asc
            """)
    List<ReviewCardEntity> findDue(@Param("userId") long userId,
                                   @Param("date") LocalDate date,
                                   Pageable pageable);

    long countByUserIdAndDueDateLessThanEqual(long userId, LocalDate date);
}
