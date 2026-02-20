package com.maplog.friend.query.dto;

import java.time.LocalDateTime;

public record FriendRequestResponse(
        Long friendId,
        Long requesterId,
        String requesterNickname,
        String requesterProfileImageUrl,
        LocalDateTime requestedAt
) {}