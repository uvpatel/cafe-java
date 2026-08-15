package com.cafepos.identity.api;

import com.cafepos.identity.application.AuthenticateUserUseCase;
import com.cafepos.identity.application.LoginCommand;
import com.cafepos.identity.application.LoginResponse;
import com.cafepos.identity.application.RefreshTokenCommand;
import com.cafepos.identity.application.RefreshTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@Tag(name = "Authentication", description = "Endpoints for user authentication and JWT token management")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;

    public AuthController(AuthenticateUserUseCase authenticateUserUseCase) {
        this.authenticateUserUseCase = Objects.requireNonNull(authenticateUserUseCase, "authenticateUserUseCase must not be null");
    }

    @Operation(summary = "Login user", description = "Authenticates an employee with credentials and issues JWT access/refresh tokens.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginCommand command) {
        LoginResponse response = authenticateUserUseCase.authenticate(command);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Refresh JWT access token", description = "Exchanges a valid refresh token for a new access token.")
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenCommand command) {
        RefreshTokenResponse response = authenticateUserUseCase.refreshToken(command);
        return ResponseEntity.ok(response);
    }
}
