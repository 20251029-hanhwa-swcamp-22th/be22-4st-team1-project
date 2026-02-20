package com.maplog.friend.query.dto;

import java.time.LocalDateTime;

public record FriendSummaryResponse(
        Long friendId,
        Long userId,
        String nickname,
        String profileImageUrl,
        LocalDateTime respondedAt
) {}