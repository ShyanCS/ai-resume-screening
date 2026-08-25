package com.hiresense.api.auth.dto;

public record LoginResponse(String accessToken, UserResponse user) {}
