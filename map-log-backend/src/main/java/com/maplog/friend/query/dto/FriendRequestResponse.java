package com.maplog.friend.query.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class FriendRequestResponse {
    private Long friendId;
    private Long requesterId;
    private String requesterNickname;
    private String requesterProfileImageUrl;
    private LocalDateTime requestedAt;
}