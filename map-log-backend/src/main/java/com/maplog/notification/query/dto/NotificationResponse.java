package com.maplog.notification.query.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String type,
        Long referenceId,
        String message,
        boolean read,
        LocalDateTime createdAt
) {}