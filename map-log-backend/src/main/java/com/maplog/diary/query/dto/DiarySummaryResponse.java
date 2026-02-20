package com.maplog.diary.query.dto;

import java.time.LocalDateTime;

public record DiarySummaryResponse(
        Long id,
        String title,
        String locationName,
        LocalDateTime visitedAt,
        String visibility,
        LocalDateTime createdAt
) {}