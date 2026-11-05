package com.spacedrep.infrastructure.persistence.adapter;

import com.spacedrep.domain.model.UserAccount;
import com.spacedrep.domain.port.UserAccountRepository;
import com.spacedrep.infrastructure.persistence.repository.UserAccountJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserAccountRepositoryAdapter implements UserAccountRepository {

    private final UserAccountJpaRepository jpa;

    public UserAccountRepositoryAdapter(UserAccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return jpa.findByEmail(email)
                .map(entity -> new UserAccount(entity.getId(), entity.getEmail(),
                        entity.getPasswordHash(), entity.getDisplayName()));
    }
}
