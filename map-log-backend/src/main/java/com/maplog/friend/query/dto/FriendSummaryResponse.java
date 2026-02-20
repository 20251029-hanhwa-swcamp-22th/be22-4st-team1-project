package com.maplog.friend.query.dto;

public record FriendSummaryResponse(
        Long friendId,
        Long userId,
        String nickname,
        String profileImageUrl
) {}