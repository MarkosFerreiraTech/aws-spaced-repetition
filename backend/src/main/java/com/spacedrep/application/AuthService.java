package com.spacedrep.application;

import com.spacedrep.application.port.TokenIssuer;
import com.spacedrep.domain.exception.InvalidCredentialsException;
import com.spacedrep.domain.model.UserAccount;
import com.spacedrep.domain.port.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;

    public AuthService(UserAccountRepository users, PasswordEncoder passwordEncoder,
                       TokenIssuer tokenIssuer) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
    }

    /**
     * Mesma mensagem para usuario inexistente e senha errada: nao vazar quais
     * e-mails existem (enumeracao de usuarios).
     */
    @Transactional(readOnly = true)
    public TokenIssuer.IssuedToken login(String email, String password) {
        UserAccount user = users.findByEmail(email == null ? "" : email.trim().toLowerCase())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return tokenIssuer.issue(user.id(), user.email());
    }
}
