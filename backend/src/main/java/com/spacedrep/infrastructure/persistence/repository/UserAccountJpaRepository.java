package com.spacedrep.infrastructure.persistence.repository;

import com.spacedrep.infrastructure.persistence.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, Long> {

    Optional<UserAccountEntity> findByEmail(String email);
}
