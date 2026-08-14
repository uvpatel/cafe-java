package com.cafepos.identity.api;

import com.cafepos.identity.application.AuthenticateUserUseCase;
import com.cafepos.identity.application.LoginCommand;
import com.cafepos.identity.application.LoginResponse;
import com.cafepos.identity.application.RefreshTokenCommand;
import com.cafepos.identity.application.RefreshTokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;

    public AuthController(AuthenticateUserUseCase authenticateUserUseCase) {
        this.authenticateUserUseCase = Objects.requireNonNull(authenticateUserUseCase, "authenticateUserUseCase must not be null");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginCommand command) {
        LoginResponse response = authenticateUserUseCase.authenticate(command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenCommand command) {
        RefreshTokenResponse response = authenticateUserUseCase.refreshToken(command);
        return ResponseEntity.ok(response);
    }
}
