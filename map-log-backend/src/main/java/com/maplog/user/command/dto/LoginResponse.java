package com.maplog.user.command.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {}