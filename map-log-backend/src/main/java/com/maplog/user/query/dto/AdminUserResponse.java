package com.maplog.user.query.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String email,
        String nickname,
        String role,
        String status,
        String suspensionReason,
        LocalDateTime createdAt
) {}
