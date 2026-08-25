package com.hiresense.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptInviteRequest(@NotBlank String token) {}
