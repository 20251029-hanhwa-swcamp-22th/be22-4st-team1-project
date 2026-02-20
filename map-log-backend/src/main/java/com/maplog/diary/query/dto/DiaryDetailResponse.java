package com.maplog.diary.query.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DiaryDetailResponse(
        Long id,
        Long userId,
        String authorNickname,
        String title,
        String content,
        Double latitude,
        Double longitude,
        String locationName,
        String address,
        LocalDateTime visitedAt,
        String visibility,
        LocalDateTime createdAt,
        boolean scraped,
        List<DiaryImageResponse> images
) {}