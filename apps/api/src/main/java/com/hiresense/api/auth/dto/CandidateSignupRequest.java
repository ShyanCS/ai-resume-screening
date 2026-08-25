package com.hiresense.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CandidateSignupRequest(
        @NotBlank @Email(message = "must be a valid email address") String email,
        @NotBlank @Size(min = 8, max = 100, message = "must be between 8 and 100 characters") String password,
        @NotBlank @Size(max = 200, message = "must be at most 200 characters") String fullName) {}
