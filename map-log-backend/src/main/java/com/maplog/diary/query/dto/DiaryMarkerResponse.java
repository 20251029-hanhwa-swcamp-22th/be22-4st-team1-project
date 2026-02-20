package com.maplog.diary.query.dto;

public record DiaryMarkerResponse(
        Long id,
        Double latitude,
        Double longitude,
        String title
) {}