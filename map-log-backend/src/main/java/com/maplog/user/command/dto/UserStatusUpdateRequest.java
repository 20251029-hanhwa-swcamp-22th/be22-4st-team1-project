package com.maplog.user.command.dto;

import com.maplog.user.command.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record UserStatusUpdateRequest(

        @NotNull(message = "상태값은 필수입니다.")
        UserStatus status,

        String suspensionReason,

        LocalDateTime suspensionExpiresAt
) {}