package com.maplog.friend.command.dto;

import com.maplog.friend.command.domain.FriendStatus;
import jakarta.validation.constraints.NotNull;

public record FriendRespondRequest(

        @NotNull(message = "응답 상태는 필수입니다.")
        FriendStatus status
) {}