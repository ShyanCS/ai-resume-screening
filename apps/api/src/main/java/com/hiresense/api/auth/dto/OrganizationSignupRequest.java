package com.hiresense.api.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizationSignupRequest(
        @NotBlank @Size(max = 200, message = "must be at most 200 characters") String orgName,
        @Size(max = 100, message = "must be at most 100 characters") String slug,
        @Valid AdminSignup admin) {

    public record AdminSignup(
            @NotBlank @Email(message = "must be a valid email address") String email,
            @NotBlank @Size(min = 8, max = 100, message = "must be between 8 and 100 characters") String password,
            @NotBlank @Size(max = 200, message = "must be at most 200 characters") String fullName) {}
}
