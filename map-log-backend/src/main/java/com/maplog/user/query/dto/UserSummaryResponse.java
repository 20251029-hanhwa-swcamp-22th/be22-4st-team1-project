package com.maplog.user.query.dto;

public record UserSummaryResponse(
        Long id,
        String nickname,
        String profileImageUrl
) {}
