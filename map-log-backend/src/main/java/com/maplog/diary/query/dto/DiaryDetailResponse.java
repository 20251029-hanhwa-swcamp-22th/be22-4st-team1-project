package com.maplog.diary.query.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DiaryDetailResponse {
    private Long id;
    private Long userId;
    private String authorNickname;
    private String title;
    private String content;
    private Double latitude;
    private Double longitude;
    private String locationName;
    private String address;
    private LocalDateTime visitedAt;
    private String visibility;
    private LocalDateTime createdAt;
    private boolean scraped;
    private List<DiaryImageResponse> images;
}