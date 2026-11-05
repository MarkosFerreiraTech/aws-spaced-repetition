package com.spacedrep.domain.port;

import com.spacedrep.domain.model.UserAccount;

import java.util.Optional;

public interface UserAccountRepository {

    Optional<UserAccount> findByEmail(String email);
}
