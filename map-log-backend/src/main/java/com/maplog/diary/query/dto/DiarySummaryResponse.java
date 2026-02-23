package com.maplog.diary.query.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class DiarySummaryResponse {
    private Long id;
    private String title;
    private String locationName;
    private LocalDateTime visitedAt;
    private String visibility;
    private LocalDateTime createdAt;
    private String authorNickname;
}