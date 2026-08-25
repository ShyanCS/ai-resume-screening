package com.hiresense.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequest(@NotBlank @Email(message = "must be a valid email address") String email) {}
