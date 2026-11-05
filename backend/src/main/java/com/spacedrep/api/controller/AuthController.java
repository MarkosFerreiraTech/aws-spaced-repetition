package com.spacedrep.api.controller;

import com.spacedrep.api.dto.LoginRequest;
import com.spacedrep.api.dto.TokenResponse;
import com.spacedrep.application.AuthService;
import com.spacedrep.application.port.TokenIssuer;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        TokenIssuer.IssuedToken issued = authService.login(request.email(), request.password());
        return new TokenResponse(issued.token(), "Bearer", issued.expiresAt());
    }
}
