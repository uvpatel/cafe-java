package com.cafepos.organization.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OnboardCafeCommand(
        @NotBlank(message = "Cafe name is required")
        @Size(max = 100, message = "Cafe name must not exceed 100 characters")
        String cafeName,

        @NotBlank(message = "Slug is required")
        @Size(max = 100, message = "Slug must not exceed 100 characters")
        String slug,

        String timezone,

        String currency,

        @NotBlank(message = "Admin name is required")
        @Size(max = 100, message = "Admin name must not exceed 100 characters")
        String adminName,

        @NotBlank(message = "Admin email is required")
        @Email(message = "Admin email must be a valid email address")
        @Size(max = 255, message = "Admin email must not exceed 255 characters")
        String adminEmail,

        @NotBlank(message = "Admin password is required")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String adminPassword
) {}
