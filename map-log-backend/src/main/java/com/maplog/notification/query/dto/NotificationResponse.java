package com.maplog.notification.query.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class NotificationResponse {
    private Long id;
    private String type;
    private Long referenceId;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
}