package com.maplog.friend.query.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class FriendSummaryResponse {
    private Long friendId;
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private LocalDateTime respondedAt;
}
