package com.cafepos.organization.api;

import com.cafepos.organization.application.OnboardCafeCommand;
import com.cafepos.organization.application.OnboardCafeResponse;
import com.cafepos.organization.application.OnboardCafeUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
public class OnboardCafeController {

    private final OnboardCafeUseCase onboardCafeUseCase;

    public OnboardCafeController(OnboardCafeUseCase onboardCafeUseCase) {
        this.onboardCafeUseCase = Objects.requireNonNull(onboardCafeUseCase, "onboardCafeUseCase must not be null");
    }

    @PostMapping("/signup")
    public ResponseEntity<OnboardCafeResponse> onboardCafe(@Valid @RequestBody OnboardCafeCommand command) {
        OnboardCafeResponse response = onboardCafeUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
