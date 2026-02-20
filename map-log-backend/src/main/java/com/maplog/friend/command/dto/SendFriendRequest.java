package com.maplog.friend.command.dto;

import jakarta.validation.constraints.NotNull;

public record SendFriendRequest(

        @NotNull(message = "상대방 ID는 필수입니다.")
        Long receiverId
) {}