package com.hiresense.api.auth.dto;

import com.hiresense.api.user.PlatformRole;

public record UserResponse(Long id, String email, String fullName, PlatformRole platformRole) {}
