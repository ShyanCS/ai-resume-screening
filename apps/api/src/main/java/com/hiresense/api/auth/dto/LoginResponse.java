package com.hiresense.api.auth.dto;

public record LoginResponse(String accessToken, String refreshToken, UserResponse user) {}
